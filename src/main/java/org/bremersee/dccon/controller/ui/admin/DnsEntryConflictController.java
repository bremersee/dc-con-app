/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bremersee.dccon.controller.ui.admin;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.components.DnsZoneTypeComponent;
import org.bremersee.dccon.controller.ui.components.PageableComponent;
import org.bremersee.dccon.controller.ui.model.DnsEntryDeleteRequest;
import org.bremersee.dccon.controller.ui.model.RedirectMessage;
import org.bremersee.dccon.controller.ui.model.RedirectMessageType;
import org.bremersee.dccon.model.DnsEntry;
import org.bremersee.dccon.model.DnsEntryType;
import org.bremersee.dccon.service.DnsService;
import org.bremersee.exception.ServiceException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The type DnsEntryEditController.
 *
 * @author Christian Bremer
 */
@Controller
@Slf4j
public class DnsEntryConflictController extends AbstractEditController implements PageableComponent,
    DnsZoneTypeComponent {

  private final DnsService dnsService;

  public DnsEntryConflictController(
      DomainControllerProperties properties,
      LocaleResolver localeResolver, DnsService dnsService) {
    super(properties, localeResolver);
    this.dnsService = dnsService;
  }

  @Override
  public String getDefaultSort() {
    return DNS_ENTRY_SORT;
  }

  @GetMapping(path = "/admin/dns-entry-conflict")
  public String displayDnsEntryConflict(
      @RequestParam(name = ZONE_NAME) String zoneName,
      @RequestParam(name = "name") String name,
      @RequestParam(name = "type") DnsEntryType type,
      @RequestParam(name = "value") String value,
      ModelMap model,
      RedirectAttributes redirectAttributes) {

    log.debug("displayDnsEntryConflict({}, {}, {}, {})", zoneName, name, type, value);
    return dnsService.findDnsEntry(new DnsEntry(zoneName, name, type, value))
        .map(entry -> {
          if (!entry.isConflict()) {
            Map<String, Object> parameters = getParamterMap();
            parameters = putToParameterMap(parameters, "name", name);
            parameters = putToParameterMap(parameters, "type", type);
            parameters = putToParameterMap(parameters, "value", value);
            return getRedirectUri("dns-entry-edit", PAGE_AND_DNS_ENTRY_PARAMS, parameters);
          }
          model.addAttribute("dnsEntry", entry);
          List<DnsEntry> dnsEntries = dnsService.findDnsEntriesConflictingWith(entry)
              .sorted(Comparator.comparing(DnsEntry::getModified).reversed())
              .toList();
          model.addAttribute("dnsEntries", dnsEntries);
          model.addAttribute("dnsEntryDeleteRequest", new DnsEntryDeleteRequest());
          return "admin/dns-entry-conflict";
        })
        .orElseGet(() -> entityNotFoundRedirect(
            redirectAttributes, "Dns Entry", "todo", name, PAGE_AND_ZONE_NAME_PARAMS,
            "dns-zone-entries"));
  }

  @PostMapping(path = "/admin/dns-entry-conflict")
  public String deleteDnsEntryConflict(
      @RequestParam(name = ZONE_NAME) String zoneName,
      @RequestParam(name = "name") String name,
      @RequestParam(name = "type") DnsEntryType type,
      @RequestParam(name = "value") String value,
      @ModelAttribute(name = "dnsEntryDeleteRequest") DnsEntryDeleteRequest deleteRequest,
      ModelMap model,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {

    log.debug("deleteDnsEntryConflict({}, {}, {}, {}, {})",
        zoneName, name, type, value, deleteRequest);

    return dnsService.findDnsEntry(new DnsEntry(zoneName, name, type, value))
        .map(entry -> {
          if (!entry.getDisplayName().equals(deleteRequest.getVerificationName())) {
            bindingResult.rejectValue("verificationName", "todo", "The name doesn't match.");
            model.addAttribute("dnsEntry", entry);
            List<DnsEntry> dnsEntries = dnsService.findDnsEntriesConflictingWith(entry)
                .sorted(Comparator.comparing(DnsEntry::getModified).reversed())
                .toList();
            model.addAttribute("dnsEntries", dnsEntries);
            return "admin/dns-entry-conflict";
          }
          model.clear();
          try {
            dnsService.deleteDnsEntry(entry);

            String msg = String.format("Dns entry '%s' was successfully deleted.", name);
            RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.SUCCESS, msg,
                "todo", name);
            redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);

          } catch (ServiceException e) {

            String msg = String.format("Deletion of dns entry '%s' failed.", name);
            log.error(msg, e);
            RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.WARNING, msg,
                "todo", name);
            redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);
          }

          Map<String, Object> parameters = getParamterMap();
          parameters = putToParameterMap(parameters, ZONE_NAME, zoneName);
          String redirect = getRedirectUri("dns-zone-entries?zone-name={{zone-name}}",
              PAGE_AND_ZONE_TYPE_PARAMS, parameters);
          logRedirectTo("Dns deletion redirect.", redirect);
          return redirect;
        })
        .orElseGet(() -> entityNotFoundRedirect(
            redirectAttributes, "Dns Entry", "todo", name, PAGE_AND_ZONE_NAME_PARAMS,
            "dns-zone-entries"));
  }

}
