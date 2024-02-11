/*
 * Copyright 2019-2020 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.comparator.spring.mapper.SortMapper;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DhcpLease;
import org.bremersee.dccon.model.DnsNode;
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.model.UnknownFilter;
import org.bremersee.dccon.repository.DhcpRepository;
import org.bremersee.dccon.repository.DnsNodeRepository;
import org.bremersee.dccon.repository.DnsZoneRepository;
import org.bremersee.dccon.repository.MockRepository;
import org.bremersee.exception.ServiceException;
import org.bremersee.pagebuilder.PageBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

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
   * @param properties the properties
   * @param dhcpRepository the dhcp repository
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
  public Page<DnsNode> query(Pageable pageable, String query, UnknownFilter unknownFilter) {
    log.info("msg=[Query dns nodes.] query=[{}] unknownFilter=[{}]", query, unknownFilter);
    if (ObjectUtils.isEmpty(query)) {
      return Page.empty();
    }
    final Set<String> ips = new HashSet<>();
    if (patternMac.matcher(query.toUpperCase()).matches()) {
      findIpByMac(query).ifPresent(ips::add);
    } else if (patternIp4.matcher(query).matches()) {
      ips.add(query);
    }
    List<DnsNode> dnsNodes;
    if (ObjectUtils.isEmpty(ips)) {
      dnsNodes = dnsNodeRepository.findByHostName(query, unknownFilter)
          .map(Collections::singletonList)
          .orElseGet(Collections::emptyList);
    } else {
      dnsNodes = dnsNodeRepository.findByIps(ips, unknownFilter);
    }
    return new PageBuilder<DnsNode, DnsNode>()
        .sourceEntries(dnsNodes)
        .pageable(SortMapper.applyDefaults(pageable, null, true, null))
        .build();
  }

  private Optional<String> findIpByMac(String mac) {
    final String normalizedMac = mac.replace("-", ":").trim();
    return dhcpRepository.findActiveByIp().values().stream()
        .filter(dhcpLease -> normalizedMac.equalsIgnoreCase(dhcpLease.getMac()))
        .map(DhcpLease::getIp)
        .findFirst();
  }

  @Override
  public Page<DhcpLease> getDhcpLeases(Pageable pageable, Boolean all) {
    Collection<DhcpLease> leases;
    if (Boolean.TRUE.equals(all)) {
      leases = dhcpRepository.findAll();
    } else {
      leases = dhcpRepository.findActiveByIp().values();
    }
    return new PageBuilder<DhcpLease, DhcpLease>()
        .sourceEntries(leases)
        .pageable(SortMapper.applyDefaults(pageable, null, true, null))
        .build();
  }

  @Override
  public Page<DnsZone> getDnsZones(Pageable pageable, Boolean reverseOnly) {
    Predicate<DnsZone> filter;
    if (ObjectUtils.isEmpty(reverseOnly)) {
      filter = zone -> true;
    } else if (Boolean.TRUE.equals(reverseOnly)) {
      filter = DnsZone::getReverseZone;
    } else {
      filter = zone -> !Boolean.TRUE.equals(zone.getDefaultZone());
    }
    return new PageBuilder<DnsZone, DnsZone>()
        .sourceEntries(dnsZoneRepository.findAll())
        .sourceFilter(filter)
        .pageable(pageable) // TODO dnsZoneComparator
        .build();
  }

  @Override
  public DnsZone addDnsZone(DnsZone dnsZone) {
    return dnsZoneRepository.save(dnsZone.getName());
  }

  @Override
  public Boolean deleteDnsZone(String zoneName) {
    final boolean success = dnsZoneRepository.delete(zoneName);
    if (success && (dnsNodeRepository instanceof MockRepository)) {
      dnsNodeRepository.deleteAll(zoneName);
    }
    return success;
  }


  @Override
  public Page<DnsNode> getDnsNodes(
      String zoneName,
      Pageable pageable,
      String query,
      UnknownFilter unknownFilter) {

    if (!dnsZoneRepository.exists(zoneName)) {
      throw ServiceException.notFoundWithErrorCode(
          DnsZone.class.getSimpleName(),
          zoneName,
          "org.bremersee:dc-con-app:e7466445-059f-4089-b69a-89bf08a9af1c");
    }
    return new PageBuilder<DnsNode, DnsNode>()
        .sourceEntries(dnsNodeRepository.findAll(zoneName, unknownFilter, query))
        .pageable(pageable) // TODO dnsNodeComparator
        .build();
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
