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
import java.util.HashSet;
import java.util.List;
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
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.model.UnknownFilter;
import org.bremersee.dccon.repository.DhcpRepository;
import org.bremersee.dccon.repository.DnsNodeRepository;
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

  private final DhcpRepository dhcpRepository;

  private final DnsZoneRepository dnsZoneRepository;

  private final DnsNodeRepository dnsNodeRepository;

  private final DnsNodeComparator dnsNodeComparator;

  private final DnsZoneComparator dnsZoneComparator;

  private final Pattern patternMac;

  private final Pattern patternIp4;

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
    this.dhcpRepository = dhcpRepository;
    this.dnsZoneRepository = dnsZoneRepository;
    this.dnsNodeRepository = dnsNodeRepository;

    this.dnsNodeComparator = new DnsNodeComparator();
    this.dnsZoneComparator = new DnsZoneComparator(this.dnsZoneRepository);

    this.patternMac = Pattern.compile(properties.getMacRegex());
    this.patternIp4 = Pattern.compile(properties.getIp4Regex());
  }

  @Override
  public List<DnsNode> query(final String query, final UnknownFilter unknownFilter) {
    log.info("msg=[Query dns nodes.] query=[{}] unknownFilter=[{}]", query, unknownFilter);
    if (!StringUtils.hasText(query)) {
      return Collections.emptyList();
    }
    final Set<String> ips = new HashSet<>();
    if (patternMac.matcher(query.toUpperCase()).matches()) {
      findIpByMac(query).ifPresent(ips::add);
    } else if (patternIp4.matcher(query).matches()) {
      ips.add(query);
    }
    if (!ips.isEmpty()) {
      return dnsNodeRepository.findByIps(ips, unknownFilter);
    }
    return dnsNodeRepository.findByHostName(query, unknownFilter)
        .map(Collections::singletonList)
        .orElseGet(Collections::emptyList);
  }

  private Optional<String> findIpByMac(String mac) {
    final String normalizedMac = mac.replace("-", ":").trim();
    return dhcpRepository.findActiveByIp().values().stream()
        .filter(dhcpLease -> normalizedMac.equalsIgnoreCase(dhcpLease.getMac()))
        .map(DhcpLease::getIp)
        .findFirst();
  }

  @Override
  public List<DhcpLease> getDhcpLeases(final Boolean all, final String sort) {
    final List<DhcpLease> leases;
    if (Boolean.TRUE.equals(all)) {
      leases = dhcpRepository.findAll();
    } else {
      leases = new ArrayList<>(dhcpRepository.findActiveByIp().values());
    }
    final String sortOrder = StringUtils.hasText(sort) ? sort : DhcpLease.SORT_ORDER_BEGIN_HOSTNAME;
    leases.sort(ComparatorBuilder.builder()
        .fromWellKnownText(sortOrder)
        .build());
    return leases;
  }

  @Override
  public List<DnsZone> getDnsZones() {
    return dnsZoneRepository.findAll()
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
  public List<DnsNode> getDnsNodes(String zoneName, UnknownFilter unknownFilter, String query) {
    if (!dnsZoneRepository.exists(zoneName)) {
      throw ServiceException.notFoundWithErrorCode(
          DnsZone.class.getSimpleName(),
          zoneName,
          "org.bremersee:dc-con-app:e7466445-059f-4089-b69a-89bf08a9af1c");
    }
    return dnsNodeRepository.findAll(zoneName, unknownFilter, query)
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
      throw ServiceException.notFoundWithErrorCode(
          DnsZone.class.getSimpleName(),
          zoneName,
          "org.bremersee:dc-con-app:97333c5a-b7e7-46a4-9034-055675d4a604");
    }
    return dnsNodeRepository.findOne(zoneName, nodeName, unknownFilter);
  }

  @Override
  public Boolean deleteDnsNode(@NotNull String zoneName, @NotNull String nodeName) {
    if (!dnsZoneRepository.exists(zoneName)) {
      throw ServiceException.notFoundWithErrorCode(
          DnsZone.class.getSimpleName(),
          zoneName,
          "org.bremersee:dc-con-app:02b09711-952e-4c05-bcef-2d1ac949b330");
    }
    return dnsNodeRepository.delete(zoneName, nodeName);
  }

  @Override
  public void deleteAllDnsNodes(@NotNull String zoneName) {
    if (!dnsZoneRepository.exists(zoneName)) {
      throw ServiceException.notFoundWithErrorCode(
          DnsZone.class.getSimpleName(),
          zoneName,
          "org.bremersee:dc-con-app:4a24a3db-7d81-4542-ac1d-68e898fed494");
    }
    dnsNodeRepository.deleteAll(zoneName);
  }

  @Override
  public void deleteAllDnsNodes(@NotNull String zoneName, List<String> nodeNames) {
    if (!dnsZoneRepository.exists(zoneName)) {
      throw ServiceException.notFoundWithErrorCode(
          DnsZone.class.getSimpleName(),
          zoneName,
          "org.bremersee:dc-con-app:937cdb01-5c89-46be-aae4-747e360b903e");
    }
    dnsNodeRepository.deleteAll(zoneName, nodeNames);
  }

}
