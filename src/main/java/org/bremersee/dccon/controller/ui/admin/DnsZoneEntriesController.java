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

import java.util.Optional;
import org.bremersee.comparator.model.SortOrders;
import org.bremersee.comparator.spring.mapper.SortMapper;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.CurrentPageNameProvider;
import org.bremersee.dccon.controller.ui.components.PageableComponent;
import org.bremersee.dccon.model.DnsEntryPage;
import org.bremersee.dccon.service.DnsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
public class DnsZoneEntriesController extends AbstractEditController
    implements CurrentPageNameProvider, PageableComponent {

  private final DnsService dnsService;

  public DnsZoneEntriesController(
      DomainControllerProperties properties,
      LocaleResolver localeResolver,
      DnsService dnsService) {
    super(properties, localeResolver);
    this.dnsService = dnsService;
  }

  @Override
  public String getCurrentPageName() {
    return "dns-zone-entries";
  }

  @Override
  public String getDefaultSort() {
    return DNS_ENTRY_SORT;
  }

  @GetMapping(path = "/admin/dns-zone-entries")
  public String displayDnsZoneEntries(
      @RequestParam(name = "zone-name", required = false) String zoneName,
      @RequestParam(name = PAGE, defaultValue = PAGE_DEFAULT) int page,
      @RequestParam(name = SIZE, defaultValue = SIZE_DEFAULT) int size,
      @RequestParam(name = SORT, defaultValue = DNS_ENTRY_SORT) SortOrders sort,
      @RequestParam(name = QUERY, required = false) String query,
      ModelMap model,
      RedirectAttributes redirectAttributes) {

    return Optional.ofNullable(zoneName)
        .flatMap(dnsService::findDnsZone)
        .map(zone -> {
          model.addAttribute("zone", zone);
          Pageable pageable = PageRequest.of(page, size, SortMapper.toSort(sort));
          DnsEntryPage dnsEntryPage = new DnsEntryPage(
              dnsService.findDnsEntries(zone.getName(), pageable, query));
          model.addAttribute("dnsEntryPage", dnsEntryPage);
          return "admin/dns-zone-entries";
        })
        .orElseGet(() -> entityNotFoundRedirect(
            redirectAttributes, "DNS Zone", "todo", zoneName, "dns-zones"));
  }

}
