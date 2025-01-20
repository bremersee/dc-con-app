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

import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.CurrentPageNameProvider;
import org.bremersee.dccon.controller.ui.components.DnsZoneTypeComponent;
import org.bremersee.dccon.controller.ui.components.PageableComponent;
import org.bremersee.dccon.service.DnsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The type DnsZoneEntriesController.
 *
 * @author Christian Bremer
 */
@Controller
public class DnsZoneInfoController extends AbstractEditController
    implements CurrentPageNameProvider, PageableComponent, DnsZoneTypeComponent {

  private final DnsService dnsService;

  public DnsZoneInfoController(
      DomainControllerProperties properties,
      LocaleResolver localeResolver,
      DnsService dnsService) {
    super(properties, localeResolver);
    this.dnsService = dnsService;
  }

  @Override
  public String getCurrentPageName() {
    return "dns-zone-info";
  }

  @Override
  public String getDefaultSort() {
    return DNS_ENTRY_SORT;
  }

  @GetMapping(path = "/admin/dns-zone-info")
  public String displayDnsZoneInfo(
      @RequestParam(name = ZONE_NAME) String zoneName,
      ModelMap model,
      RedirectAttributes redirectAttributes) {

    return dnsService.findDnsZone(zoneName)
        .map(zone -> {
          model.addAttribute("zone", zone);
          return "admin/dns-zone-info";
        })
        .orElseGet(() -> entityNotFoundRedirect(
            redirectAttributes, "DNS Zone", "todo", zoneName, PAGE_AND_ZONE_TYPE_PARAMS,
            "dns-zones"));
  }

}
