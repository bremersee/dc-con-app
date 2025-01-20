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
import org.bremersee.dccon.controller.ui.CurrentPageNameProvider;
import org.bremersee.dccon.controller.ui.components.DnsZoneTypeComponent;
import org.bremersee.dccon.controller.ui.components.PageableComponent;
import org.bremersee.dccon.controller.ui.model.DnsZoneDeleteRequest;
import org.bremersee.dccon.controller.ui.model.RedirectMessage;
import org.bremersee.dccon.controller.ui.model.RedirectMessageType;
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
 * The type DnsZoneEntriesController.
 *
 * @author Christian Bremer
 */
@Controller
@Slf4j
public class DnsZoneDeleteController extends AbstractEditController
    implements CurrentPageNameProvider, PageableComponent, DnsZoneTypeComponent {

  private final DnsService dnsService;

  public DnsZoneDeleteController(
      DomainControllerProperties properties,
      LocaleResolver localeResolver,
      DnsService dnsService) {
    super(properties, localeResolver);
    this.dnsService = dnsService;
  }

  @Override
  public String getCurrentPageName() {
    return "dns-zone-delete";
  }

  @Override
  public String getDefaultSort() {
    return DNS_ENTRY_SORT;
  }

  @GetMapping(path = "/admin/dns-zone-delete")
  public String displayDnsZoneInfo(
      @RequestParam(name = ZONE_NAME) String zoneName,
      ModelMap model) {

    model.addAttribute("zoneName", zoneName);
    model.addAttribute("dnsZoneDeleteRequest", new DnsZoneDeleteRequest());
    return "admin/dns-zone-delete";
  }

  @PostMapping(path = "/admin/dns-zone-delete")
  public String deleteDnsZone(
      @RequestParam(name = ZONE_NAME) String zoneName,
      @ModelAttribute(name = "dnsZoneDeleteRequest") DnsZoneDeleteRequest deleteRequest,
      ModelMap model,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {

    log.debug("deleteDnsEntry({}, {})", zoneName, deleteRequest);

    if (!zoneName.equalsIgnoreCase(deleteRequest.getVerificationName())) {
      bindingResult.rejectValue("verificationName", "todo", "The name doesn't match.");
      model.addAttribute("zoneName", zoneName);
      return "admin/dns-zone-delete";
    }

    model.clear();
    try {
      dnsService.deleteDnsZone(zoneName);

      String msg = String.format("Dns zone '%s' was successfully deleted.", zoneName);
      RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.SUCCESS, msg,
          "todo", zoneName);
      redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);

    } catch (ServiceException e) {

      String msg = String.format("Deletion of dns zone '%s' failed.", zoneName);
      log.error(msg, e);
      RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.WARNING, msg,
          "todo", zoneName);
      redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);
    }

    Map<String, Object> parameters = getParamterMap();
    String redirect = getRedirectUri("dns-zones",
        PAGE_AND_ZONE_TYPE_PARAMS, parameters);
    logRedirectTo("Dns zone deletion redirect.", redirect);
    return redirect;
  }

}
