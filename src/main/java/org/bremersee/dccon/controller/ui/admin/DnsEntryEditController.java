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
import org.bremersee.dccon.controller.ui.model.DnsEntryEditRequest;
import org.bremersee.dccon.controller.ui.model.RedirectMessage;
import org.bremersee.dccon.controller.ui.model.RedirectMessageType;
import org.bremersee.dccon.model.DnsEntry;
import org.bremersee.dccon.model.DnsEntryType;
import org.bremersee.dccon.service.DnsService;
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
public class DnsEntryEditController extends AbstractEditController implements PageableComponent,
    DnsZoneTypeComponent {

  private final DnsService dnsService;

  public DnsEntryEditController(
      DomainControllerProperties properties,
      LocaleResolver localeResolver, DnsService dnsService) {
    super(properties, localeResolver);
    this.dnsService = dnsService;
  }

  @Override
  public String getDefaultSort() {
    return DNS_ENTRY_SORT;
  }

  @GetMapping(path = "/admin/dns-entry-edit")
  public String displayEditDnsEntry(
      @RequestParam(name = ZONE_NAME, required = false) String zoneName,
      @RequestParam(name = "name", required = false) String name,
      @RequestParam(name = "type", required = false) DnsEntryType type,
      @RequestParam(name = "value", required = false) String value,
      @RequestParam(name = "cnf", defaultValue = "false") boolean isConflict,
      @RequestParam(name = "guid", required = false) String objectGuid,
      ModelMap model,
      RedirectAttributes redirectAttributes) {

    log.debug("displayEditDnsEntry({}, {}, {}, {}, {}, {})",
        zoneName, name, type, value, isConflict, objectGuid);
    if (isConflict) {
      // TODO
    }
    return dnsService.findDnsEntry(zoneName, name, type, value)
        .map(dnsEntry -> {
          model.addAttribute("zoneName", zoneName);
          model.addAttribute("dnsEntry", dnsEntry);
          model.addAttribute("dnsEntryEditRequest", new DnsEntryEditRequest(dnsEntry));
          return "admin/dns-entry-edit";
        })
        .orElseGet(() -> entityNotFoundRedirect(
            redirectAttributes, "DNS Entry", "todo", zoneName, PAGE_AND_ZONE_TYPE_PARAMS,
            "dns-zone-entries"));
  }

  @PostMapping(path = "/admin/dns-entry-edit")
  public String updateDnsEntry(
      @RequestParam(name = ZONE_NAME) String zoneName,
      @RequestParam(name = "name") String name,
      @RequestParam(name = "type") DnsEntryType type,
      @RequestParam(name = "value") String value,
      @ModelAttribute(name = "dnsEntryEditRequest") DnsEntryEditRequest dnsEntryEditRequest,
      ModelMap model,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {

    log.debug("updateDnsEntry()");
    DnsEntry dnsEntry = new DnsEntry();
    dnsEntry.setName(name);
    dnsEntry.setType(type);
    dnsEntry.setValue(value);
    DnsEntry updatedDnsEntry = dnsService
        .updateDnsEntry(zoneName, dnsEntry, dnsEntryEditRequest.getNewDnsEntryValue());
    model.clear();
    String msg = String.format("Dns entry '%s' was successfully updated.",
        updatedDnsEntry.getName());
    RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.SUCCESS, msg,
        "todo", updatedDnsEntry.getName());
    redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);

    Map<String, Object> parameters = getParamterMap();
    parameters = putToParameterMap(parameters, ZONE_NAME, zoneName);
    parameters = putToParameterMap(parameters, "name", updatedDnsEntry.getName());
    parameters = putToParameterMap(parameters, "type", updatedDnsEntry.getType());
    parameters = putToParameterMap(parameters, "value", updatedDnsEntry.getValue());
    String redirect = getRedirectUri("dns-entry-edit",
        PAGE_AND_DNS_ENTRY_PARAMS, parameters);
    logRedirectTo("Dns entry successfully updated.", redirect);
    return redirect;
  }

}
