/*
 * Copyright 2019 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.comparator.ComparatorBuilder;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DhcpLease;
import org.bremersee.dccon.model.DnsNode;
import org.bremersee.dccon.model.DnsRecord;
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.model.UnknownFilter;
import org.bremersee.dccon.repository.DhcpRepository;
import org.bremersee.dccon.repository.DnsNodeRepository;
import org.bremersee.dccon.repository.DnsRecordType;
import org.bremersee.dccon.repository.DnsZoneRepository;
import org.bremersee.exception.ServiceException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * The name server service.
 *
 * @author Christian Bremer
 */
@Component("nameServerService")
@Slf4j
public class NameServerServiceImpl implements NameServerService {

  private final DomainControllerProperties properties;

  private final DhcpRepository dhcpRepository;

  private final DnsZoneRepository dnsZoneRepository;

  private final DnsNodeRepository dnsNodeRepository;

  private final DnsNodeComparator dnsNodeComparator;

  private final DnsZoneComparator dnsZoneComparator;

  /**
   * Instantiates a new name server service.
   *
   * @param properties        the properties
   * @param dhcpRepository    the dhcp repository
   * @param dnsZoneRepository the dns zone repository
   * @param dnsNodeRepository the dns node repository
   */
  public NameServerServiceImpl(
      final DomainControllerProperties properties,
      final DhcpRepository dhcpRepository,
      final DnsZoneRepository dnsZoneRepository,
      final DnsNodeRepository dnsNodeRepository) {
    this.properties = properties;
    this.dhcpRepository = dhcpRepository;
    this.dnsZoneRepository = dnsZoneRepository;
    this.dnsNodeRepository = dnsNodeRepository;

    this.dnsNodeComparator = new DnsNodeComparator();
    this.dnsZoneComparator = new DnsZoneComparator(this.dnsZoneRepository);
  }

  @Override
  public List<DnsNode> query(final String query, final UnknownFilter unknownFilter) {
    final Set<String> ips = new HashSet<>();
    if (Pattern.compile(properties.getMacRegex()).matcher(query).matches()) {
      ips.addAll(dhcpRepository.findIpByMac(query));
    } else if (Pattern.compile(properties.getIp4Regex()).matcher(query).matches()) {
      ips.add(query);
    }
    if (!ips.isEmpty()) {
      return dnsNodeRepository.findByIps(ips, unknownFilter);
    }
    return dnsNodeRepository.findByHostName(query, unknownFilter)
        .map(Collections::singletonList)
        .orElse(Collections.emptyList());
  }

  @Override
  public List<DhcpLease> getDhcpLeases(final Boolean all, final String sort) {
    final List<DhcpLease> leases;
    if (Boolean.TRUE.equals(all)) {
      leases = dhcpRepository.findAll();
    } else {
      leases = dhcpRepository.findActive();
    }
    final String sortOrder = StringUtils.hasText(sort) ? sort : DhcpLease.SORT_ORDER_BEGIN_HOSTNAME;
    leases.sort(ComparatorBuilder.builder()
        .fromWellKnownText(sortOrder)
        .build());
    return leases;
  }

  private Map<String, List<DhcpLease>> getDhcpLeasesMap(
      final String zoneName) {
    final Map<String, List<DhcpLease>> leasesMap;
    if (dnsZoneRepository.isDnsReverseZone(zoneName)) {
      leasesMap = Collections.emptyMap();
    } else {
      leasesMap = new HashMap<>();
      final List<DhcpLease> leasesList = getDhcpLeases(
          false,
          DhcpLease.SORT_ORDER_IP_BEGIN_HOSTNAME);
      for (DhcpLease lease : leasesList) {
        leasesMap.computeIfAbsent(lease.getIp(), key -> new ArrayList<>()).add(lease);
      }
    }
    return leasesMap;
  }

  private DnsNode addDhcpLeases(
      final Map<String, List<DhcpLease>> leasesMap,
      final DnsNode dnsNode) {

    return dnsNode.getRecords().stream()
        .filter(dnsRecord -> dnsRecord.getRecordValue() != null
            && (DnsRecordType.A.is(dnsRecord.getRecordType())
            || DnsRecordType.AAAA.is(dnsRecord.getRecordType())))
        .min(ComparatorBuilder.builder()
            .fromWellKnownText(DnsRecord.SORT_ORDER_TIME_STAMP_DESC)
            .build())
        .map(dnsRecord -> {
          leasesMap
              .getOrDefault(dnsRecord.getRecordValue(), Collections.emptyList()).stream()
              .findFirst()
              .ifPresent(dnsRecord::setDhcpLease);
          return dnsNode;
        })
        .orElse(dnsNode);
  }

  private boolean isNonExcludedDnsZone(final DnsZone zone) {
    return zone != null && !isExcludedDnsZone(zone);
  }

  private boolean isExcludedDnsZone(final DnsZone zone) {
    return zone != null && isExcludedDnsZone(zone.getName());
  }

  private boolean isExcludedDnsZone(final String zoneName) {
    return zoneName != null && properties.getExcludedZoneRegexList().stream()
        .anyMatch(regex -> Pattern.compile(regex).matcher(zoneName).matches());
  }

  @Override
  public List<DnsZone> getDnsZones() {
    return dnsZoneRepository.findAll()
        .filter(this::isNonExcludedDnsZone)
        .sorted(dnsZoneComparator)
        .collect(Collectors.toList());
  }

  @Override
  public DnsZone addDnsZone(DnsZone dnsZone) {
    return dnsZoneRepository.save(dnsZone.getName());
  }

  @Override
  public Boolean deleteDnsZone(String zoneName) {
    return dnsZoneRepository.delete(zoneName);
  }


  @Override
  public List<DnsNode> getDnsNodes(String zoneName, UnknownFilter unknownFilter) {
    if (!dnsZoneRepository.exists(zoneName)) {
      throw ServiceException.notFound(DnsZone.class.getSimpleName(), zoneName);
    }
    final Map<String, List<DhcpLease>> leasesMap = getDhcpLeasesMap(zoneName);
    return dnsNodeRepository.findAll(zoneName, unknownFilter)
        .filter(this::isNonExcludedDnsNode)
        .map(dnsEntry -> addDhcpLeases(leasesMap, dnsEntry))
        .sorted(dnsNodeComparator)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<DnsNode> save(String zoneName, DnsNode dnsNode) {
    if (!dnsZoneRepository.exists(zoneName)) {
      throw ServiceException.notFound(DnsZone.class.getSimpleName(), zoneName);
    }
    return dnsNodeRepository.save(zoneName, dnsNode);
  }

  @Override
  public Optional<DnsNode> getDnsNode(
      String zoneName,
      String nodeName,
      UnknownFilter unknownFilter) {
    if (!dnsZoneRepository.exists(zoneName)) {
      throw ServiceException.notFound(DnsZone.class.getSimpleName(), zoneName);
    }
    return dnsNodeRepository.findOne(zoneName, nodeName, unknownFilter);
  }

  @Override
  public Boolean deleteDnsNode(@NotNull String zoneName, @NotNull String nodeName) {
    if (!dnsZoneRepository.exists(zoneName)) {
      throw ServiceException.notFound(DnsZone.class.getSimpleName(), zoneName);
    }
    return dnsNodeRepository.delete(zoneName, nodeName);
  }

  @Override
  public void deleteAllDnsNodes(@NotNull String zoneName) {
    if (!dnsZoneRepository.exists(zoneName)) {
      throw ServiceException.notFound(DnsZone.class.getSimpleName(), zoneName);
    }
    dnsNodeRepository.deleteAll(zoneName);
  }

  @Override
  public void deleteAllDnsNodes(@NotNull String zoneName, List<String> nodeNames) {
    if (!dnsZoneRepository.exists(zoneName)) {
      throw ServiceException.notFound(DnsZone.class.getSimpleName(), zoneName);
    }
    dnsNodeRepository.deleteAll(zoneName, nodeNames);
  }

  private boolean isNonExcludedDnsNode(final DnsNode dnsNode) {
    return dnsNode != null && !isExcludedDnsNode(dnsNode);
  }

  private boolean isExcludedDnsNode(final DnsNode dnsNode) {
    return dnsNode != null && isExcludedDnsNode(dnsNode.getName());
  }

  private boolean isExcludedDnsNode(final String nodeName) {
    return nodeName != null && properties.getExcludedNodeRegexList().stream()
        .anyMatch(regex -> Pattern.compile(regex).matcher(nodeName).matches());
  }

}
