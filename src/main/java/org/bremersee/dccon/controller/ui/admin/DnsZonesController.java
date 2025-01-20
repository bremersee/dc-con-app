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

import java.util.List;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.AbstractController;
import org.bremersee.dccon.controller.ui.components.DnsZoneTypeComponent;
import org.bremersee.dccon.controller.ui.components.DnsZoneTypeNavigationComponent;
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.model.DnsZoneType;
import org.bremersee.dccon.service.DnsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;

/**
 * The type DnsZonesController.
 *
 * @author Christian Bremer
 */
@Controller
public class DnsZonesController extends AbstractController
    implements DnsZoneTypeNavigationComponent, DnsZoneTypeComponent {

  private final DnsService dnsService;

  public DnsZonesController(
      DomainControllerProperties properties,
      LocaleResolver localeResolver,
      DnsService dnsService) {
    super(properties, localeResolver);
    this.dnsService = dnsService;
  }

  @Override
  public String getCurrentPageName() {
    return "dns-zones";
  }

  @Override
  public String getDefaultSort() {
    return "";
  }

  @GetMapping(path = "/admin/dns-zones")
  public String displayDnsZones(
      @RequestParam(name = ZONE_TYPE, defaultValue = ZONE_TYPE_DEFAULT) DnsZoneType zoneType,
      ModelMap model) {
    List<String> dnsZones = dnsService.findDnsZoneNames(zoneType);
    model.addAttribute("dnsZoneNames", dnsZones);
    return "admin/dns-zones";
  }

}
