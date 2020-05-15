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

package org.bremersee.dccon.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DnsNode;
import org.bremersee.dccon.model.DnsRecord;
import org.bremersee.dccon.model.UnknownFilter;
import org.bremersee.exception.ServiceException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * The dns node repository mock.
 *
 * @author Christian Bremer
 */
@Profile("!ldap")
@Component
@Slf4j
public class DnsNodeRepositoryMock extends AbstractDnsNodeRepository implements MockRepository {

  private static final String NODES_LOCATION = "classpath:demo/nodes.json";

  private static final String REVERSE_ZONE = "1.168.192.in-addr.arpa";

  private static final String DISTINGUISHED_NAME_TEMPLATE = "DC=%s,DC=%s,"
      + "CN=MicrosoftDNS,DC=DomainDnsZones,DC=samdom,DC=example,DC=org";

  private final ResourceLoader resourceLoader = new DefaultResourceLoader();

  private final Map<String, Map<String, DnsNode>> repo = new ConcurrentHashMap<>();

  private final ObjectMapper objectMapper;

  /**
   * Instantiates a new dns node repository mock.
   *
   * @param properties the properties
   * @param dhcpRepository the dhcp repository
   * @param dnsZoneRepository the dns zone repository
   * @param objectMapperBuilder the object mapper builder
   */
  public DnsNodeRepositoryMock(
      DomainControllerProperties properties,
      DhcpRepository dhcpRepository,
      DnsZoneRepository dnsZoneRepository,
      Jackson2ObjectMapperBuilder objectMapperBuilder) {
    super(properties, null, dhcpRepository, dnsZoneRepository);
    this.objectMapper = objectMapperBuilder.build();
  }

  /**
   * Init.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void init() {
    log.warn("\n"
        + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n"
        + "!! MOCK is running:  DnsNodeRepository                                              !!\n"
        + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
    );
    resetData();
  }

  @Override
  public void resetData() {

    DnsNode[] dnsNodes;
    try {
      dnsNodes = objectMapper.readValue(
          resourceLoader.getResource(NODES_LOCATION).getInputStream(),
          DnsNode[].class);

    } catch (IOException e) {
      throw ServiceException.internalServerError("Loading demo data failed.", e);
    }

    Set<String> zones = new HashSet<>();
    zones.add(getProperties().getDefaultZone());
    zones.add(REVERSE_ZONE);
    getDnsZoneRepository().findAll().forEach(dnsZone -> {
      if (!zones.contains(dnsZone.getName())) {
        getDnsZoneRepository().delete(dnsZone.getName());
      }
    });
    zones.forEach(zone -> getDnsZoneRepository().save(zone));

    for (DnsNode dnsNode : dnsNodes) {
      save(getProperties().getDefaultZone(), dnsNode);
    }
  }

  @Override
  public Stream<DnsNode> findAll(String zoneName, UnknownFilter unknownFilter, String query) {
    if (query == null || query.trim().length() == 0) {
      return getNodeMap(zoneName).values().stream()
          .filter(this::isNonExcludedDnsNode)
          .map(dnsNode -> insertCorrelationValues(zoneName, dnsNode))
          .map(dnsNode -> insertDhcpLeases(zoneName, dnsNode));
    } else {
      return getNodeMap(zoneName).values().stream()
          .filter(this::isNonExcludedDnsNode)
          .map(dnsNode -> build(dnsNode, zoneName))
          .map(dnsNode -> insertCorrelationValues(zoneName, dnsNode))
          .map(dnsNode -> insertDhcpLeases(zoneName, dnsNode))
          .filter(dnsNode -> this.isQueryResult(dnsNode, query));
    }
  }

  @Override
  public boolean exists(String zoneName, String nodeName, UnknownFilter unknownFilter) {
    return getNodeMap(zoneName).containsKey(nodeName.toLowerCase());
  }

  @Override
  Optional<DnsNode> findOne(
      final String zoneName,
      final String nodeName,
      final UnknownFilter unknownFilter,
      final boolean withCorrelationValues,
      final boolean withDhcpLeases) {

    return Optional.ofNullable(build(getNodeMap(zoneName).get(nodeName.toLowerCase()), zoneName))
        .filter(this::isNonExcludedDnsNode)
        .map(dnsNode -> withCorrelationValues
            ? insertCorrelationValues(zoneName, dnsNode)
            : dnsNode)
        .map(dnsNode -> withDhcpLeases
            ? insertDhcpLeases(zoneName, dnsNode)
            : dnsNode);
  }

  @Override
  public Optional<DnsNode> save(String zoneName, DnsNode dnsNode) {
    Assert.hasText(dnsNode.getName(), "Node name must be present.");

    if (dnsNode.getRecords().isEmpty()) {
      delete(zoneName, dnsNode.getName());
      return Optional.empty();
    }

    final DnsNode newDnsNode = dnsNode.toBuilder()
        .distinguishedName(null)
        .records(dnsNode.getRecords().stream()
            .filter(dnsRecord -> StringUtils.hasText(dnsRecord.getRecordValue())
                && StringUtils.hasText(dnsRecord.getRecordType()))
            .map(dnsRecord -> dnsRecord.toBuilder()
                .recordRawValue(null)
                .correlatedRecordValue(null)
                .dhcpLease(null)
                .build())
            .collect(Collectors.toSet()))
        .build();
    if (newDnsNode.getCreated() == null) {
      newDnsNode.setCreated(OffsetDateTime.now());
    }
    if (newDnsNode.getModified() == null) {
      newDnsNode.setModified(newDnsNode.getCreated());
    }

    final Set<DnsRecord> deletedRecords = new LinkedHashSet<>();
    final Set<DnsRecord> newRecords = new LinkedHashSet<>();

    final DnsNode oldDnsNode = getNodeMap(zoneName).get(newDnsNode.getName().toLowerCase());
    if (oldDnsNode == null) {
      newRecords.addAll(newDnsNode.getRecords());
    } else {
      for (DnsRecord dnsRecord : oldDnsNode.getRecords()) {
        if (!newDnsNode.getRecords().contains(dnsRecord)) {
          deletedRecords.add(dnsRecord);
        }
      }
      for (DnsRecord dnsRecord : newDnsNode.getRecords()) {
        if (!oldDnsNode.getRecords().contains(dnsRecord)) {
          newRecords.add(dnsRecord);
        }
      }
    }

    getNodeMap(zoneName).put(newDnsNode.getName().toLowerCase(), newDnsNode);
    handlePtrRecords(zoneName, newDnsNode.getName(), newRecords, deletedRecords);
    return findOne(zoneName, newDnsNode.getName(), null, true, true);
  }

  @Override
  public boolean delete(String zoneName, DnsNode node) {
    boolean result = getNodeMap(zoneName).remove(node.getName().toLowerCase()) != null;
    if (result) {
      handlePtrRecords(zoneName, node.getName(), Collections.emptySet(), node.getRecords());
    }
    return result;
  }

  private Map<String, DnsNode> getNodeMap(String zoneName) {
    return repo.computeIfAbsent(zoneName.toLowerCase(), key -> new ConcurrentHashMap<>());
  }

  private DnsNode build(DnsNode dnsNode, String zoneName) {
    return dnsNode == null ? null : dnsNode.toBuilder()
        .distinguishedName(String.format(DISTINGUISHED_NAME_TEMPLATE, dnsNode.getName(), zoneName))
        .build();
  }

}
