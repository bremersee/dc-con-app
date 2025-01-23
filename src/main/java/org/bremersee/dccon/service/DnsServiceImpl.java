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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.bremersee.comparator.spring.mapper.SortMapper.applyDefaults;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
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
import org.ldaptive.dn.Dn;
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

  private static final Pattern IPV4_PATTERN = Pattern.compile(
      "^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)" +
          "(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$");

  private final DnsRepository dnsRepository;

  public DnsServiceImpl(DnsRepository dnsRepository) {
    this.dnsRepository = dnsRepository;
  }

  @Override
  public List<String> findDnsZoneNames(DnsZoneType type) {
    if (isNull(type)) {
      return dnsRepository.findDnsZoneNames(DnsZoneType.PRIMARY);
    }
    return dnsRepository.findDnsZoneNames(type);
  }

  @Override
  public DnsZone findDnsZone(String zoneName) {
    return dnsRepository.findDnsZone(zoneName);

  }

  @Override
  public DnsZone createDnsZone(String zoneName) {
    return dnsRepository.createDnsZone(zoneName);
  }

  @Override
  public void deleteDnsZone(String zoneName) {
    dnsRepository.deleteDnsZone(zoneName);
  }


  private boolean isQueryResult(DnsEntry entry, String query) {
    if (isEmpty(entry)) {
      return false;
    }
    if (isEmpty(query)) {
      return true;
    }
    String q = query.toLowerCase();
    StringTokenizer st = new StringTokenizer(q, " ");
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      if (nonNull(entry.getName()) && entry.getName().toLowerCase().contains(token)) {
        return true;
      }
      if (nonNull(entry.getType()) && entry.getType().name().toLowerCase().contains(token)) {
        return true;
      }
      if (nonNull(entry.getValue()) && entry.getValue().toLowerCase().contains(token)) {
        return true;
      }
    }
    return false;
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
    return dnsRepository.findDnsEntries(zoneName, name, type);
  }

  @Override
  public Optional<DnsEntry> findDnsEntry(String zoneName, String name, DnsEntryType type,
      String value) {
    return findDnsEntries(zoneName, name, type)
        .filter(dnsEntry -> dnsEntry.getType().getToSambaToolValueTransformer()
            .apply(dnsEntry.getValue()).equalsIgnoreCase(dnsEntry.getType()
                .getToSambaToolValueTransformer().apply(value)))
        .filter(dnsEntry -> !dnsEntry.getConflict())
        .findFirst()
        .map(entry -> dnsRepository
            .setCommonAttributes(findDnsZone(entry.getZoneName()).getDn(), entry));
  }

  private Optional<DnsEntry> findReverseDnsEntryOfA(DnsEntry dnsEntry) {
    return findDnsZoneNames(DnsZoneType.REVERSE).stream()
        .flatMap(zone -> findDnsEntries(zone, ZONE_ENTRIES_NODE_NAME, DnsEntryType.PTR))
        .filter(entry -> !entry.getConflict())
        .filter(entry -> dnsEntry.getValue().toLowerCase()
            .contains(entry.getName().toLowerCase())) // '192.168.1.122' contains '122'
        .filter(entry -> entry.getValue()
            .equalsIgnoreCase(
                dnsEntry.getName() + '.' + dnsEntry.getZoneName())) // 'hostname.zone-name'
        .findFirst()
        .map(entry -> dnsRepository
            .setCommonAttributes(findDnsZone(entry.getZoneName()).getDn(), entry));
  }

  private Optional<DnsEntry> findReverseDnsEntryOfPtr(DnsEntry dnsEntry) {
    DnsEntryType type = getDnsEntryTypeFromReverseZone(dnsEntry.getZoneName());
    if (isNull(type)) {
      return Optional.empty();
    }
    return findDnsZoneNames(DnsZoneType.PRIMARY).stream()
        .map(this::findDnsZone)
        .filter(zone -> !zone.getReverseZone())
        .flatMap(zone -> findDnsEntries(zone.getName(), ZONE_ENTRIES_NODE_NAME, type)
            .filter(entry -> !entry.getConflict())
            .filter(entry -> entry.getValue().toLowerCase()
                .contains(dnsEntry.getName().toLowerCase()))
            .filter(entry -> dnsEntry.getValue()
                .equalsIgnoreCase(entry.getName() + '.' + zone.getName()))
            .findFirst()
            .map(entry -> dnsRepository.setCommonAttributes(zone.getDn(), entry))
            .stream())
        .findFirst();
  }

  private DnsEntryType getDnsEntryTypeFromReverseZone(String zoneName) {
    if (isNull(zoneName) || !zoneName.toLowerCase().endsWith(REVERSE_ZONE_POSTFIX)) {
      return null;
    }
    String tmp = zoneName.substring(0, zoneName.length() - REVERSE_ZONE_POSTFIX.length());
    String[] parts = tmp.split(Pattern.quote("."));
    if (parts.length > 3) {
      return DnsEntryType.AAAA;
    }
    List<String> ipList = new ArrayList<>(4);
    for (int i = parts.length - 1; i >= 0; i--) {
      ipList.add(parts[i]);
    }
    for (int i = 3 - parts.length; i >= 0; i--) {
      ipList.add("1");
    }
    String ip = String.join(".", ipList);
    if (IPV4_PATTERN.matcher(ip).matches()) {
      return DnsEntryType.A;
    }
    return DnsEntryType.AAAA;
  }

  @Override
  public Optional<DnsEntry> findReverseDnsEntry(DnsEntry dnsEntry) {
    if (DnsEntryType.A.equals(dnsEntry.getType()) || DnsEntryType.AAAA.equals(dnsEntry.getType())) {
      return findReverseDnsEntryOfA(dnsEntry);
    } else if (DnsEntryType.PTR.equals(dnsEntry.getType())) {
      return findReverseDnsEntryOfPtr(dnsEntry);
    }
    return Optional.empty();
  }

  @Override
  public Stream<DnsEntry> findDnsEntriesWithConflict(DnsEntry dnsEntry) {
    Dn zoneDn = findDnsZone(dnsEntry.getZoneName()).getDn();
    return dnsRepository.findDnsEntryWithConflict(zoneDn, dnsEntry)
        .stream()
        .flatMap(cnfEntry -> Stream.concat(
            Stream.of(cnfEntry),
            findPossibleConflicts(zoneDn, dnsEntry)));
  }

  private Stream<DnsEntry> findPossibleConflicts(Dn zoneDn, DnsEntry dnsEntry) {
    String zoneName = zoneDn.getRDn().getNameValue().getStringValue();
    return dnsRepository.findDnsEntries(zoneName, ZONE_ENTRIES_NODE_NAME, dnsEntry.getType())
        .filter(entry -> !entry.getConflict())
        .filter(entry -> isQueryResult(entry, dnsEntry.getName() + " " + dnsEntry.getValue()))
        .map(entry -> dnsRepository.setCommonAttributes(zoneDn, entry));
  }

  @Override
  public DnsEntry addDnsEntry(DnsEntry entry) {
    String zoneName = entry.getZoneName();
    return findDnsEntry(zoneName, entry.getName(), entry.getType(), entry.getValue())
        .orElseGet(() -> {
          dnsRepository.addDnsEntry(entry);
          return findDnsEntry(zoneName, entry.getName(), entry.getType(), entry.getValue())
              .orElseThrow(() -> ServiceException.internalServerError(
                  String.format("Creating dns entry '%s' in zone '%s' failed.",
                      entry.getName(), zoneName),
                  EC_ADDING_DNS_ENTRY_FAILED));
        });
  }

  @Override
  public DnsEntry updateDnsEntry(DnsEntry entry, String newValue) {
    String zoneName = entry.getZoneName();
    if (findDnsEntry(zoneName, entry.getName(), entry.getType(), entry.getValue()).isEmpty()) {
      throw ServiceException.notFoundWithErrorCode("DnsEntry", entry.getName(),
          EC_DNS_ENTRY_NOT_FOUND);
    }
    dnsRepository.updateDnsEntry(entry, newValue);
    return findDnsEntry(zoneName, entry.getName(), entry.getType(), newValue)
        .orElseThrow(() -> ServiceException.internalServerError(
            String.format("Updating dns entry '%s' in zone '%s' failed.",
                entry.getName(), zoneName),
            EC_UPDATING_DNS_ENTRY_FAILED));
  }

  @Override
  public void deleteDnsEntry(DnsEntry entry) {
    dnsRepository.deleteDnsEntry(entry);
    String zoneName = entry.getZoneName();
    if (findDnsEntry(zoneName, entry.getName(), entry.getType(), entry.getValue()).isPresent()) {
      throw ServiceException.internalServerError(
          String.format("Deleting dns entry '%s' in zone '%s' failed.",
              entry.getName(), zoneName),
          EC_DELETING_DNS_ENTRY_FAILED);
    }
  }

}
