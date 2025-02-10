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
import static java.util.Objects.requireNonNullElse;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.comparator.ValueComparator;
import org.bremersee.comparator.spring.mapper.SortMapper;
import org.bremersee.dccon.ErrorCode;
import org.bremersee.dccon.model.DhcpLease;
import org.bremersee.dccon.model.DnsEntry;
import org.bremersee.dccon.model.DnsEntryType;
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.model.DnsZoneType;
import org.bremersee.dccon.repository.DhcpRepository;
import org.bremersee.dccon.repository.DnsEntryRepository;
import org.bremersee.dccon.repository.DnsZoneRepository;
import org.bremersee.pagebuilder.PageBuilder;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

  private static final Pattern IPV4_PATTERN = Pattern.compile(
      "^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$");

  private final SortMapper sortMapper;

  private final DnsZoneRepository dnsZoneRepository;

  private final DnsEntryRepository dnsEntryRepository;

  private final DhcpRepository dhcpRepository;

  private final CacheManager cacheManager;

  public DnsServiceImpl(
      SortMapper sortMapper,
      DnsZoneRepository dnsZoneRepository,
      DnsEntryRepository dnsEntryRepository,
      DhcpRepository dhcpRepository,
      CacheManager cacheManager) {
    this.sortMapper = sortMapper;
    this.dnsZoneRepository = dnsZoneRepository;
    this.dnsEntryRepository = dnsEntryRepository;
    this.dhcpRepository = dhcpRepository;
    this.cacheManager = cacheManager;
  }

  @Override
  public List<String> getDnsZoneNames(DnsZoneType type) {
    if (isNull(type)) {
      return dnsZoneRepository.getDnsZoneNames(DnsZoneType.PRIMARY);
    }
    return dnsZoneRepository.getDnsZoneNames(type);
  }

  @Override
  public DnsZone getDnsZone(String zoneName) {
    return dnsZoneRepository.getDnsZone(zoneName);

  }

  @CacheEvict(value = "dnsZoneListCache", allEntries = true)
  @Override
  public DnsZone createDnsZone(String zoneName) {
    return dnsZoneRepository.createDnsZone(zoneName);
  }

  @CacheEvict(value = "dnsZoneListCache", allEntries = true)
  @Override
  public void deleteDnsZone(String zoneName) {
    dnsZoneRepository.deleteDnsZone(zoneName);
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
  public Page<DnsEntry> getDnsEntries(
      String zoneName,
      Pageable pageable,
      String query) {

    if (isEmpty(query) && pageable.getPageNumber() == 0) {
      Optional.ofNullable(cacheManager.getCache("dnsEntryListCache"))
          .ifPresent(Cache::invalidate);
    }
    return new PageBuilder<DnsEntry, DnsEntry>()
        .sourceEntries(dnsEntryRepository.getDnsEntries(zoneName))
        .sourceFilter(dnsEntry -> isQueryResult(dnsEntry, query))
        .pageable(sortMapper.applyDefaults(pageable, null, true, null))
        .targetSortFn(sortOrderItem -> {
          if ("name".equalsIgnoreCase(sortOrderItem.getField())) {
            return (Comparator<DnsEntry>) (o1, o2) -> {
              String n1 = requireNonNullElse(o1.getName(), "").toLowerCase();
              String n2 = requireNonNullElse(o2.getName(), "").toLowerCase();
              return String.format("%255s", n1).compareTo(String.format("%255s", n2));
            };
          }
          return new ValueComparator(sortOrderItem);
        })
        .build();
  }

  private Stream<DnsEntry> findDnsEntries(String zoneName, String name, DnsEntryType type) {
    return dnsEntryRepository.getDnsEntries(zoneName).stream()
        .filter(entry -> DnsEntryRepository.ZONE_ENTRIES_NODE_NAME.equals(name)
            || entry.getName().equalsIgnoreCase(name))
        .filter(entry -> DnsEntryType.ALL.equals(type) || entry.getType().equals(type));
  }

  @Override
  public Optional<DnsEntry> findDnsEntry(DnsEntry dnsEntry) {
    return findDnsEntries(dnsEntry.getZoneName(), dnsEntry.getName(), dnsEntry.getType())
        .filter(entry -> entry.getType().getToSambaToolValueTransformer()
            .apply(entry.getValue()).equalsIgnoreCase(dnsEntry.getType()
                .getToSambaToolValueTransformer().apply(dnsEntry.getValue())))
        .findFirst()
        .map(dnsEntryRepository::addCommonAttributes);
  }

  @Override
  public Optional<DnsEntry> findDnsEntry(String ipAddress) {
    PageRequest pageRequest = PageRequest.of(0, Integer.MAX_VALUE);
    String query = "";
    return getDnsZoneNames(DnsZoneType.PRIMARY).stream()
        .map(this::getDnsZone)
        .filter(zone -> !zone.getReverseZone())
        .flatMap(zone -> getDnsEntries(zone.getName(), pageRequest, query).stream())
        .filter(dnsEntry -> dnsEntry.getValue().equalsIgnoreCase(ipAddress)
            && (DnsEntryType.A.equals(dnsEntry.getType())
            || DnsEntryType.AAAA.equals(dnsEntry.getType())))
        .findFirst();
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

  private Optional<DnsEntry> findReverseDnsEntryOfA(DnsEntry dnsEntry) {
    return getDnsZoneNames(DnsZoneType.REVERSE).stream()
        .flatMap(zone -> findDnsEntries(zone, DnsEntryRepository.ZONE_ENTRIES_NODE_NAME,
            DnsEntryType.PTR))
        .filter(entry -> !entry.isConflict())
        .filter(entry -> dnsEntry.getValue().toLowerCase()
            .contains(entry.getName().toLowerCase())) // '192.168.1.122' contains '122'
        .filter(entry -> entry.getValue()
            .equalsIgnoreCase(
                dnsEntry.getName() + '.' + dnsEntry.getZoneName())) // 'hostname.zone-name'
        .findFirst()
        .map(dnsEntryRepository::addCommonAttributes);
  }

  private Optional<DnsEntry> findReverseDnsEntryOfPtr(DnsEntry dnsEntry) {
    DnsEntryType type = getDnsEntryTypeFromReverseZone(dnsEntry.getZoneName());
    if (isNull(type)) {
      return Optional.empty();
    }
    return getDnsZoneNames(DnsZoneType.PRIMARY).stream()
        .map(this::getDnsZone)
        .filter(zone -> !zone.getReverseZone())
        .flatMap(
            zone -> findDnsEntries(zone.getName(), DnsEntryRepository.ZONE_ENTRIES_NODE_NAME, type)
                .filter(entry -> !entry.isConflict())
                .filter(entry -> entry.getValue().toLowerCase()
                    .contains(dnsEntry.getName().toLowerCase()))
                .filter(entry -> dnsEntry.getValue()
                    .equalsIgnoreCase(entry.getName() + '.' + zone.getName()))
                .findFirst()
                .map(dnsEntryRepository::addCommonAttributes)
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
  public Stream<DnsEntry> findDnsEntriesConflictingWith(DnsEntry dnsEntry) {
    return findDnsEntries(dnsEntry.getZoneName(), DnsEntryRepository.ZONE_ENTRIES_NODE_NAME,
        dnsEntry.getType())
        .filter(entry -> !entry.isConflict())
        .filter(entry -> isQueryResult(entry, dnsEntry.getName() + " " + dnsEntry.getValue()))
        .map(dnsEntryRepository::addCommonAttributes);
  }

  @Override
  public void addDnsEntry(DnsEntry entry) {
    dnsEntryRepository.addDnsEntry(entry);
  }

  @Override
  public void updateDnsEntry(DnsEntry entry, String newValue) {
    dnsEntryRepository.updateDnsEntry(entry, newValue);
  }

  @Override
  public void deleteDnsEntry(DnsEntry entry) {
    dnsEntryRepository.deleteDnsEntry(entry);
  }


  private boolean isQueryResult(DhcpLease lease, String query) {
    if (isEmpty(lease)) {
      return false;
    }
    if (isEmpty(query)) {
      return true;
    }
    String q = query.toLowerCase();
    StringTokenizer st = new StringTokenizer(q, " ");
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      if (nonNull(lease.getIp()) && lease.getIp().toLowerCase().contains(token)) {
        return true;
      }
      if (nonNull(lease.getHostname()) && lease.getHostname().toLowerCase().contains(token)) {
        return true;
      }
      if (nonNull(lease.getMac()) && lease.getMac().toLowerCase().contains(token)) {
        return true;
      }
      if (nonNull(lease.getManufacturer())
          && lease.getManufacturer().toLowerCase().contains(token)) {
        return true;
      }
    }
    return false;
  }

  public Page<DhcpLease> getDhcpLeases(Pageable pageable, String query) {

    if (isEmpty(query) && pageable.getPageNumber() == 0) {
      Optional.ofNullable(cacheManager.getCache("dhcpLeasesCache"))
          .ifPresent(Cache::invalidate);
    }
    return new PageBuilder<DhcpLease, DhcpLease>()
        .sourceEntries(dhcpRepository.findAll())
        .sourceFilter(dhcpLease -> isQueryResult(dhcpLease, query))
        .pageable(sortMapper.applyDefaults(pageable, null, true, null))
        .build();
  }

}
