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

import static org.bremersee.dccon.model.UnknownFilter.ALL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.data.ldaptive.LdaptiveEntryMapper;
import org.bremersee.data.ldaptive.LdaptiveTemplate;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DnsNode;
import org.bremersee.dccon.model.DnsRecord;
import org.bremersee.dccon.model.UnknownFilter;
import org.bremersee.dccon.repository.cli.CommandExecutor;
import org.bremersee.dccon.repository.ldap.DnsNodeLdapMapper;
import org.bremersee.exception.ServiceException;
import org.ldaptive.FilterTemplate;
import org.ldaptive.SearchRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * The dns node repository.
 *
 * @author Christian Bremer
 */
@Profile("ldap")
@Component("dnsNodeRepository")
@Slf4j
public class DnsNodeRepositoryImpl extends AbstractDnsNodeRepository {

  private final Map<String, LdaptiveEntryMapper<DnsNode>> dnsNodeLdapMapperMap;

  private DnsNodeLdapMapperProvider dnsNodeLdapMapperProvider;

  /**
   * Instantiates a new dns node repository.
   *
   * @param properties the properties
   * @param ldapTemplateProvider the ldap template provider
   * @param dhcpRepository the dhcp repository
   * @param dnsZoneRepository the dns zone repository
   */
  public DnsNodeRepositoryImpl(
      final DomainControllerProperties properties,
      final ObjectProvider<LdaptiveTemplate> ldapTemplateProvider,
      final DhcpRepository dhcpRepository,
      final DnsZoneRepository dnsZoneRepository) {
    super(properties, ldapTemplateProvider.getIfAvailable(), dhcpRepository, dnsZoneRepository);
    this.dnsNodeLdapMapperMap = new ConcurrentHashMap<>();
    this.dnsNodeLdapMapperProvider = (zoneName, unknownFilter) -> new DnsNodeLdapMapper(
        getProperties(), zoneName, unknownFilter);
  }

  /**
   * Keep dhcp lease caches up to date.
   */
  @Scheduled(fixedDelay = 30000L, initialDelay = 2000)
  public void keepDhcpLeaseCachesUpToDate() {
    log.trace("msg=[Keeping dhcp lease cache up to date.]");
    getDhcpRepository().findActiveByIp();
    getDhcpRepository().findActiveByHostName();
  }

  private LdaptiveEntryMapper<DnsNode> getDnsNodeLdapMapper(
      final String zoneName,
      final UnknownFilter unknownFilter) {
    final UnknownFilter filter = unknownFilter(unknownFilter);
    final String key = zoneName + ":" + filter.name();
    return dnsNodeLdapMapperMap.computeIfAbsent(
        key,
        k -> dnsNodeLdapMapperProvider.getDnsNodeLdapMapper(zoneName, filter));
  }

  /**
   * Sets dns node ldap mapper provider.
   *
   * @param dnsNodeLdapMapperProvider the dns node ldap mapper provider
   */
  @SuppressWarnings("unused")
  public void setDnsNodeLdapMapperProvider(
      final DnsNodeLdapMapperProvider dnsNodeLdapMapperProvider) {
    if (dnsNodeLdapMapperProvider != null) {
      this.dnsNodeLdapMapperProvider = dnsNodeLdapMapperProvider;
    }
  }

  @Override
  public Stream<DnsNode> findAll(
      final String zoneName,
      final UnknownFilter unknownFilter,
      final String query) {

    SearchRequest searchRequest = SearchRequest.builder()
        .dn(getProperties().buildDnsNodeBaseDn(zoneName))
        .filter(getProperties().getDnsNodeFindAllFilter())
        .scope(getProperties().getDnsNodeFindAllSearchScope())
        .binaryAttributes("dnsRecord")
        .build();
    if (query == null || query.trim().length() == 0) {
      return getLdapTemplate()
          .findAll(searchRequest, getDnsNodeLdapMapper(zoneName, unknownFilter))
          .filter(this::isNonExcludedDnsNode)
          .map(dnsNode -> insertCorrelationValues(zoneName, dnsNode))
          .map(dnsNode -> insertDhcpLeases(zoneName, dnsNode));
    } else {
      return getLdapTemplate()
          .findAll(searchRequest, getDnsNodeLdapMapper(zoneName, unknownFilter))
          .filter(this::isNonExcludedDnsNode)
          .map(dnsNode -> insertCorrelationValues(zoneName, dnsNode))
          .map(dnsNode -> insertDhcpLeases(zoneName, dnsNode))
          .filter(dnsNode -> this.isQueryResult(dnsNode, query));
    }
  }

  @Override
  public boolean exists(
      final String zoneName,
      final String nodeName,
      final UnknownFilter unknownFilter) {
    return isNonExcludedDnsNode(nodeName)
        && getDnsZoneRepository().exists(zoneName)
        && getLdapTemplate().exists(DnsNode.builder().name(nodeName).build(),
        getDnsNodeLdapMapper(zoneName, unknownFilter));
  }

  @Override
  Optional<DnsNode> findOne(
      final String zoneName,
      final String nodeName,
      final UnknownFilter unknownFilter,
      final boolean withCorrelationValues,
      final boolean withDhcpLeases) {

    SearchRequest searchRequest = SearchRequest.builder()
        .dn(getProperties().buildDnsNodeBaseDn(zoneName))
        .filter(FilterTemplate.builder()
            .filter(getProperties().getDnsNodeFindOneFilter())
            .parameters(nodeName)
            .build())
        .scope(getProperties().getDnsNodeFindAllSearchScope())
        .binaryAttributes("dnsRecord")
        .build();
    return getLdapTemplate().findOne(searchRequest, getDnsNodeLdapMapper(zoneName, unknownFilter))
        .filter(this::isNonExcludedDnsNode)
        .map(dnsNode -> withCorrelationValues
            ? insertCorrelationValues(zoneName, dnsNode)
            : dnsNode)
        .map(dnsNode -> withDhcpLeases
            ? insertDhcpLeases(zoneName, dnsNode)
            : dnsNode);
  }

  @Override
  public Optional<DnsNode> save(
      final String zoneName,
      final DnsNode dnsNode) {

    if (isExcludedDnsNode(dnsNode)) {
      throw ServiceException.badRequest(
          "Node name is not allowed.",
          "org.bremersee:dc-con-app:8dd7165e-89af-4423-900a-5fc0a71fe7bf");
    }
    // Collect deleted records and save existing dns node
    final Set<DnsRecord> deletedRecords = new LinkedHashSet<>();
    DnsNode newDnsNode = findOne(zoneName, dnsNode.getName(), ALL, false, false)
        .map(existingDnsNode -> {
          for (final DnsRecord existingDnsRecord : existingDnsNode.getRecords()) {
            if (!dnsNode.getRecords().contains(existingDnsRecord)) {
              deletedRecords.add(existingDnsRecord);
            }
          }
          if (deletedRecords.size() == existingDnsNode.getRecords().size()) {
            getLdapTemplate().remove(
                existingDnsNode,
                getDnsNodeLdapMapper(zoneName, ALL));
            return DnsNode.builder()
                .name(dnsNode.getName())
                .build();
          }
          return getLdapTemplate().save(dnsNode, getDnsNodeLdapMapper(zoneName, ALL));
        })
        .orElseGet(() -> DnsNode.builder()
            .name(dnsNode.getName())
            .build());

    // Collect new records
    final Set<DnsRecord> newRecords = new LinkedHashSet<>();
    for (final DnsRecord record : dnsNode.getRecords()) {
      if (!newDnsNode.getRecords().contains(record)) {
        newRecords.add(record);
      }
    }

    if (newDnsNode.getRecords().isEmpty() && newRecords.isEmpty()) {
      // The dns node has no records, it will be deleted
      if (StringUtils.hasText(newDnsNode.getDistinguishedName())) {
        getLdapTemplate().remove(dnsNode, getDnsNodeLdapMapper(zoneName, ALL));
      }
      newDnsNode = null;
    } else {
      // Add new record via cli
      add(zoneName, dnsNode.getName(), newRecords);
      // Load dns node from ldap
      newDnsNode = findOne(zoneName, dnsNode.getName(), ALL, false, false)
          .orElseThrow(() -> ServiceException.internalServerError(
              "Saving dns node failed.",
              "org.bremersee:dc-con-app:7eabb994-f6db-49dc-870b-b4e2dd330a4c"));
    }

    // Do A record to PTR record synchronization
    handlePtrRecords(zoneName, dnsNode.getName(), newRecords, deletedRecords);

    return Optional.ofNullable(newDnsNode);
  }

  /**
   * Add a dns node.
   *
   * @param zoneName the zone name
   * @param nodeName the node name
   * @param records the records
   */
  void add(
      final String zoneName,
      final String nodeName,
      final Collection<DnsRecord> records) {

    if (zoneName == null || nodeName == null || records == null || records.isEmpty()) {
      return;
    }

    kinit();
    for (final DnsRecord record : records) {
      final List<String> commands = new ArrayList<>();
      sudo(commands);
      commands.add(getProperties().getSambaToolBinary());
      commands.add("dns");
      commands.add("add");
      commands.add(getProperties().getNameServerHost());
      commands.add(zoneName);
      commands.add(nodeName);
      commands.add(record.getRecordType());
      commands.add(record.getRecordValue());
      auth(commands);
      CommandExecutor.exec(commands, getProperties().getSambaToolExecDir());
    }
  }

  @Override
  public boolean delete(final String zoneName, final DnsNode node) {
    if (isExcludedDnsNode(node)) {
      throw ServiceException.badRequest(
          "Node name is not allowed.",
          "org.bremersee:dc-con-app:3e377240-eafd-45ea-9ee6-048ab3ca8cec");
    }
    getLdapTemplate().remove(node, getDnsNodeLdapMapper(zoneName, ALL));
    handlePtrRecords(zoneName, node.getName(), Collections.emptySet(), node.getRecords());
    return true;
  }

}
