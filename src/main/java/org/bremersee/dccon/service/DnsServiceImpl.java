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

import static java.util.Objects.nonNull;
import static org.bremersee.comparator.spring.mapper.SortMapper.applyDefaults;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.ErrorCode;
import org.bremersee.dccon.model.DnsEntry;
import org.bremersee.dccon.model.DnsEntryType;
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.model.DnsZoneType;
import org.bremersee.dccon.repository.DnsRepository;
import org.bremersee.exception.ServiceException;
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
@Slf4j
public class DnsServiceImpl implements DnsService, ErrorCode {

  private static final String ZONE_ENTRIES_NODE_NAME = "@";

  private final DnsRepository dnsRepository;

  public DnsServiceImpl(DnsRepository dnsRepository) {
    this.dnsRepository = dnsRepository;
  }

  @Override
  public List<String> findDnsZoneNames(DnsZoneType type) {
    return dnsRepository.findDnsZoneNames(type);
  }

  @Override
  public Optional<DnsZone> findDnsZone(String zoneName) {
    return dnsRepository.findDnsZone(zoneName);
  }

  @Override
  public DnsZone createDnsZone(String zoneName) {
    return findDnsZone(zoneName)
        .orElseGet(() -> {
          dnsRepository.createDnsZone(zoneName);
          return findDnsZone(zoneName)
              .orElseThrow(() -> ServiceException.internalServerError(
                  String.format("Creating dns zone '%s' failed.", zoneName),
                  EC_CREATING_DNS_ZONE_FAILED));
        });
  }

  @Override
  public void deleteDnsZone(String zoneName) {
    dnsRepository.deleteDnsZone(zoneName);
    if (findDnsZone(zoneName).isPresent()) {
      throw ServiceException.internalServerError(
          String.format("Deleting dns zone '%s' failed.", zoneName),
          EC_DELETING_DNS_ZONE_FAILED);
    }
  }


  private boolean isQueryResult(DnsEntry entry, String query) {
    if (isEmpty(entry)) {
      return false;
    }
    if (isEmpty(query)) {
      return true;
    }
    String q = query.toLowerCase();
    if (nonNull(entry.getName()) && entry.getName().toLowerCase().contains(q)) {
      return true;
    }
    if (nonNull(entry.getType()) && entry.getType().name().toLowerCase().contains(q)) {
      return true;
    }
    return nonNull(entry.getValue()) && entry.getValue().toLowerCase().contains(q);
  }

  @Override
  public Page<DnsEntry> findDnsEntries(
      String zoneName,
      Pageable pageable,
      String query) {

    return new PageBuilder<DnsEntry, DnsEntry>()
        .sourceEntries(findDnsEntries(zoneName, ZONE_ENTRIES_NODE_NAME, DnsEntryType.ALL))
        .sourceFilter(dnsEntry -> isQueryResult(dnsEntry, query))
        .pageable(applyDefaults(pageable, null, true, null))
        .build();
  }

  @Override
  public Stream<DnsEntry> findDnsEntries(String zoneName, String name, DnsEntryType type) {
    return dnsRepository.findDnsEntries(new DnsZone(zoneName), name, type);
  }

  @Override
  public Optional<DnsEntry> findDnsEntry(String zoneName, String name, DnsEntryType type,
      String value) {
    return findDnsEntries(zoneName, name, type)
        .filter(dnsEntry -> dnsEntry.getValue().equalsIgnoreCase(value))
        .filter(dnsEntry -> !dnsEntry.getConflict())
        .findFirst(); // TODO how to find and handle conflicts?
  }

  @Override
  public DnsEntry addDnsEntry(String zoneName, DnsEntry entry) {
    return findDnsEntry(zoneName, entry.getName(), entry.getType(), entry.getValue())
        .orElseGet(() -> {
          dnsRepository.addDnsEntry(zoneName, entry);
          return findDnsEntry(zoneName, entry.getName(), entry.getType(), entry.getValue())
              .orElseThrow(() -> ServiceException.internalServerError(
                  String.format("Creating dns entry '%s' in zone '%s' failed.",
                      entry.getName(), zoneName),
                  EC_ADDING_DNS_ENTRY_FAILED));
        });
  }

  @Override
  public DnsEntry updateDnsEntry(String zoneName, DnsEntry entry, String newValue) {
    if (findDnsEntry(zoneName, entry.getName(), entry.getType(), entry.getValue()).isEmpty()) {
      throw ServiceException.notFoundWithErrorCode("DnsEntry", entry.getName(),
          EC_DNS_ENTRY_NOT_FOUND);
    }
    dnsRepository.updateDnsEntry(zoneName, entry, newValue);
    return findDnsEntry(zoneName, entry.getName(), entry.getType(), newValue)
        .orElseThrow(() -> ServiceException.internalServerError(
            String.format("Updating dns entry '%s' in zone '%s' failed.",
                entry.getName(), zoneName),
            EC_UPDATING_DNS_ENTRY_FAILED));
  }

  @Override
  public void deleteDnsEntry(String zoneName, DnsEntry entry) {
    dnsRepository.deleteDnsEntry(zoneName, entry);
    if (findDnsEntry(zoneName, entry.getName(), entry.getType(), entry.getValue()).isPresent()) {
      throw ServiceException.internalServerError(
          String.format("Deleting dns entry '%s' in zone '%s' failed.",
              entry.getName(), zoneName),
          EC_DELETING_DNS_ENTRY_FAILED);
    }
  }

}
