/*
 * Copyright 2024 the original author or authors.
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

import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.bremersee.comparator.model.SortOrders;
import org.bremersee.comparator.spring.mapper.SortMapper;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.AbstractController;
import org.bremersee.dccon.controller.ui.CurrentPageNameProvider;
import org.bremersee.dccon.controller.ui.components.PageableComponent;
import org.bremersee.dccon.controller.ui.components.RedirectComponent;
import org.bremersee.dccon.controller.ui.model.RedirectMessage;
import org.bremersee.dccon.controller.ui.model.RedirectMessageType;
import org.bremersee.dccon.model.DhcpLeasePage;
import org.bremersee.dccon.model.DnsZoneType;
import org.bremersee.dccon.service.DnsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;

/**
 * The type DhcpLeasesController.
 *
 * @author Christian Bremer
 */
@Controller
public class DhcpLeasesController extends AbstractController
    implements PageableComponent, CurrentPageNameProvider, RedirectComponent {

  private final DnsService dnsService;

  public DhcpLeasesController(
      DomainControllerProperties domainControllerProperties,
      LocaleResolver localeResolver,
      DnsService dnsService) {
    super(domainControllerProperties, localeResolver);
    this.dnsService = dnsService;
  }

  @Override
  public String getDefaultSort() {
    return DHCP_LEASE_SORT;
  }

  @Override
  public String getCurrentPageName() {
    return "dhcp-leases";
  }

  @RequestMapping(path = "/admin/dhcp-leases", method = RequestMethod.GET)
  public String displayDhcpLeases(
      @RequestParam(name = PAGE, defaultValue = PAGE_DEFAULT) int page,
      @RequestParam(name = SIZE, defaultValue = SIZE_DEFAULT) int size,
      @RequestParam(name = SORT, defaultValue = DHCP_LEASE_SORT) SortOrders sort,
      @RequestParam(name = QUERY, required = false) String query,
      @RequestParam(name = "ip", required = false) String ipAddress,
      ModelMap model) {

    return Optional.ofNullable(ipAddress)
        .filter(ip -> !ip.isEmpty())
        .flatMap(dnsService::findDnsEntry)
        .map(dnsEntry -> {
          Map<String, Object> parameters = new HashMap<>();
          parameters.put(PAGE, 0);
          parameters.put(SIZE, SIZE_DEFAULT_INT);
          parameters.put(SORT, DNS_ENTRY_SORT);
          parameters.put(QUERY, "");
          parameters.put(ZONE_TYPE, DnsZoneType.PRIMARY);
          parameters.put(ZONE_NAME, dnsEntry.getZoneName());
          parameters.put("name", dnsEntry.getName());
          parameters.put("type", dnsEntry.getType());
          parameters.put("value", dnsEntry.getValue());
          return getRedirectUri("dns-entry-edit", PAGE_AND_DNS_ENTRY_PARAMS, parameters);
        })
        .orElseGet(() -> {
          Pageable pageable = PageRequest.of(page, size, SortMapper.toSort(sort));
          DhcpLeasePage dhcpLeasePage = new DhcpLeasePage(
              dnsService.getDhcpLeases(pageable, query));
          model.addAttribute("dhcpLeasePage", dhcpLeasePage);
          if (!isEmpty(ipAddress)) {
            model.addAttribute("rmsg",
                new RedirectMessage("No dns entry with ip address '" + ipAddress + "' was found.",
                    RedirectMessageType.WARNING));
          }
          return "admin/dhcp-leases";
        });
  }

}
