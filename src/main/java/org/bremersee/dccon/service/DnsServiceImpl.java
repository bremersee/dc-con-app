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

package org.bremersee.dccon.service;

import static org.bremersee.comparator.spring.mapper.SortMapper.applyDefaults;

import java.util.Optional;
import java.util.stream.Stream;
import org.bremersee.dccon.model.DnsEntry;
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.model.DnsZoneEntries;
import org.bremersee.dccon.model.DnsZoneType;
import org.bremersee.dccon.repository.DnsRepository;
import org.bremersee.pagebuilder.PageBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * The type DnsServiceImpl.
 *
 * @author Christian Bremer
 */
@Service
public class DnsServiceImpl implements DnsService {

  private final DnsRepository dnsRepository;

  public DnsServiceImpl(DnsRepository dnsRepository) {
    this.dnsRepository = dnsRepository;
  }

  @Override
  public Stream<DnsZone> findDnsZones(DnsZoneType type) {
    return dnsRepository.findDnsZones(type);
  }

  @Override
  public Optional<DnsZone> findDnsZone(String zoneName) {
    return dnsRepository.findDnsZone(zoneName);
  }

  @Override
  public boolean existsDnsZone(String zoneName) {
    return dnsRepository.existsDnsZone(zoneName);
  }

  @Override
  public DnsZone createDnsZone(String zoneName) {
    return dnsRepository.createDnsZone(zoneName);
  }

  @Override
  public boolean deleteDnsZone(String zoneName) {
    return dnsRepository.deleteDnsZone(zoneName);
  }

  @Override
  public Optional<DnsZoneEntries<Page<DnsEntry>>> findDnsEntries(
      String zoneName,
      Pageable pageable,
      String query) {

    return dnsRepository.findDnsEntries(zoneName, query)
        .map(dnsZoneEntries -> {
          DnsZoneEntries<Page<DnsEntry>> page = new DnsZoneEntries<>(Page::empty);
          Page<DnsEntry> entryPage = new PageBuilder<DnsEntry, DnsEntry>()
              .sourceEntries(dnsZoneEntries.getDnsEntries())
              .pageable(applyDefaults(pageable, null, true, null))
              .build();
          page.setDnsEntries(entryPage);
          return page;
        });
  }
}
