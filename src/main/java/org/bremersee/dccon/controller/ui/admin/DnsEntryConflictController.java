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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
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
import org.bremersee.exception.ServiceException;
import org.ldaptive.LdapUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.ObjectUtils;
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
      @RequestParam(name = "guid") String objectGuid,
      ModelMap model) {

    log.debug("displayDnsEntryConflict({}, {}, {}, {}, {})",
        zoneName, name, type, value, objectGuid);

    DnsEntry entry = new DnsEntry();
    entry.setName(name);
    entry.setType(type);
    entry.setValue(value);
    entry.setConflict(true);
    entry.setObjectGuid(objectGuid);
    List<DnsEntry> dnsEntries = dnsService.findDnsEntriesWithConflict(zoneName, entry)
        .sorted(Comparator.comparing(DnsEntry::getModified).reversed())
        .toList();
    model.addAttribute("zoneName", zoneName);
    model.addAttribute("dnsEntry", entry);
    model.addAttribute("dnsEntries", dnsEntries);
    return "admin/dns-entry-conflict";
  }

  @PostMapping(path = "/admin/dns-entry-conflict")
  public String deleteDnsEntries(
      @RequestParam(name = ZONE_NAME) String zoneName,
      @RequestParam(name = "dns-entry-id") List<String> internalIds,
      ModelMap model,
      RedirectAttributes redirectAttributes) {

    log.debug("deleteDnsEntries({}, {})", zoneName, internalIds);

    List<DnsEntry> dnsEntries = Stream.ofNullable(internalIds)
        .flatMap(Collection::stream)
        .filter(id -> !ObjectUtils.isEmpty(id))
            .map(DnsEntry::fromInternalId)
        .toList();

    log.debug("deleteDnsEntries({}, {})", zoneName, dnsEntries);

    return "redirect:dns-entries?zoneName=" + zoneName;
  }

}
