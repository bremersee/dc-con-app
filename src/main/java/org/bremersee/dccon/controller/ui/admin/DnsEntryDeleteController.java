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
 * The type DnsEntryDeleteController.
 *
 * @author Christian Bremer
 */
@Controller
@Slf4j
public class DnsEntryDeleteController extends AbstractEditController implements PageableComponent,
    DnsZoneTypeComponent {

  private final DnsService dnsService;

  public DnsEntryDeleteController(
      DomainControllerProperties properties,
      LocaleResolver localeResolver, DnsService dnsService) {
    super(properties, localeResolver);
    this.dnsService = dnsService;
  }

  @Override
  public String getDefaultSort() {
    return DNS_ENTRY_SORT;
  }

  @GetMapping(path = "/admin/dns-entry-delete")
  public String displayDeleteDnsEntry(
      @RequestParam(name = ZONE_NAME, required = false) String zoneName,
      @RequestParam(name = "name", required = false) String name,
      @RequestParam(name = "type", required = false) DnsEntryType type,
      @RequestParam(name = "value", required = false) String value,
      ModelMap model,
      RedirectAttributes redirectAttributes) {

    log.debug("displayDeleteDnsEntry({}, {}, {}, {})", zoneName, name, type, value);
    return dnsService.findDnsEntry(zoneName, name, type, value)
        .map(dnsEntry -> {
          model.addAttribute("zoneName", zoneName);
          model.addAttribute("dnsEntry", dnsEntry);
          model.addAttribute("dnsEntryDeleteRequest", new DnsEntryDeleteRequest());
          return "admin/dns-entry-delete";
        })
        .orElseGet(() -> entityNotFoundRedirect(
            redirectAttributes, "DNS Entry", "todo", zoneName, PAGE_AND_ZONE_TYPE_PARAMS,
            "dns-zone-entries"));
  }

  @PostMapping(path = "/admin/dns-entry-delete")
  public String deleteDnsEntry(
      @RequestParam(name = ZONE_NAME) String zoneName,
      @RequestParam(name = "name") String name,
      @RequestParam(name = "type") DnsEntryType type,
      @RequestParam(name = "value") String value,
      @ModelAttribute(name = "dnsEntryDeleteRequest") DnsEntryDeleteRequest deleteRequest,
      ModelMap model,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {

    log.debug("deleteDnsEntry({}, {}, {}, {}, {})", zoneName, name, type, value, deleteRequest);

    DnsEntry dnsEntry = new DnsEntry();
    dnsEntry.setName(name);
    dnsEntry.setType(type);
    dnsEntry.setValue(value);

    if (!name.equalsIgnoreCase(deleteRequest.getVerificationName())) {
      bindingResult.rejectValue("verificationName", "todo", "The name doesn't match.");
      model.addAttribute("zoneName", zoneName);
      model.addAttribute("dnsEntry", dnsEntry);
      return "admin/dns-entry-delete";
    }

    model.clear();
    try {
      dnsService.deleteDnsEntry(zoneName, dnsEntry);

      String msg = String.format("Dns entry '%s' was successfully deleted.", name);
      RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.SUCCESS, msg,
          "todo", name);
      redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);

    } catch (ServiceException e) {

      log.error("Deleting dns entry failed.", e);

      String msg = String.format("Deletion of dns entry '%s' failed.", name);
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
  }

}
