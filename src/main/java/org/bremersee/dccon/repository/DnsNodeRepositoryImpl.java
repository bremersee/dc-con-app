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

package org.bremersee.dccon.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.data.ldaptive.LdaptiveTemplate;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DnsNode;
import org.bremersee.dccon.model.DnsPair;
import org.bremersee.dccon.model.DnsRecord;
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.repository.cli.CommandExecutor;
import org.bremersee.dccon.repository.ldap.DnsNodeLdapMapper;
import org.bremersee.exception.ServiceException;
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * The dns node repository.
 *
 * @author Christian Bremer
 */
@Profile("ldap")
@Component("dnsNodeRepository")
@Slf4j
public class DnsNodeRepositoryImpl extends AbstractRepository implements DnsNodeRepository {

  private final DnsZoneRepository dnsZoneRepository;

  private final Map<String, DnsNodeLdapMapper> dnsNodeLdapMapperMap = new ConcurrentHashMap<>();

  /**
   * Instantiates a new dns node repository.
   *
   * @param properties        the properties
   * @param ldapTemplate      the ldap template
   * @param dnsZoneRepository the dns zone repository
   */
  public DnsNodeRepositoryImpl(
      final DomainControllerProperties properties,
      final LdaptiveTemplate ldapTemplate,
      final DnsZoneRepository dnsZoneRepository) {
    super(properties, ldapTemplate);
    this.dnsZoneRepository = dnsZoneRepository;
  }

  private DnsNodeLdapMapper getDnsNodeLdapMapper(final String zoneName) {
    return dnsNodeLdapMapperMap
        .computeIfAbsent(zoneName,
            key -> new DnsNodeLdapMapper(getProperties(), getProperties().buildDnsNodeBaseDn(key)));
  }

  @Override
  public Stream<DnsNode> findAll(@NotNull final String zoneName) {
    log.info("msg=[findAll] zoneName=[{}]", zoneName);
    final SearchRequest searchRequest = new SearchRequest(
        getProperties().buildDnsNodeBaseDn(zoneName),
        new SearchFilter(getProperties().getDnsNodeFindAllFilter()));
    searchRequest.setSearchScope(getProperties().getDnsNodeFindAllSearchScope());
    searchRequest.setBinaryAttributes("dnsRecord");
    return getLdapTemplate().findAll(searchRequest, getDnsNodeLdapMapper(zoneName));
  }

  @Override
  public boolean exists(@NotNull final String zoneName, @NotNull final String nodeName) {
    return dnsZoneRepository.exists(zoneName) && getLdapTemplate().exists(
        DnsNode.builder().name(nodeName).build(),
        getDnsNodeLdapMapper(zoneName));
  }

  @Override
  public Optional<DnsNode> findOne(@NotNull final String zoneName, @NotNull final String nodeName) {
    final SearchFilter searchFilter = new SearchFilter(getProperties().getDnsNodeFindOneFilter());
    searchFilter.setParameter(0, nodeName);
    final SearchRequest searchRequest = new SearchRequest(
        getProperties().buildDnsNodeBaseDn(zoneName),
        searchFilter);
    searchRequest.setSearchScope(getProperties().getDnsNodeFindAllSearchScope());
    searchRequest.setBinaryAttributes("dnsRecord");
    return getLdapTemplate().findOne(searchRequest, getDnsNodeLdapMapper(zoneName));
  }

  @Override
  public Optional<DnsPair> findReverseDnsNode(
      @NotNull final String zoneName,
      @NotNull final DnsRecord record) {

    if (dnsZoneRepository.isDnsReverseZone(zoneName)) {
      return Optional.empty();
    }
    if (DnsRecordType.A.name().equalsIgnoreCase(record.getRecordType())) {
      final String ip4 = record.getRecordValue();
      return findDnsZoneByIp4(ip4).map(reverseZone -> buildDnsPairByIp4(ip4, reverseZone));
    }
    return Optional.empty();
  }

  private DnsPair buildDnsPairByIp4(String ip4, DnsZone reverseZone) {
    final String nodeName = getDnsNodeNameByIp4(ip4, reverseZone.getName());
    return findOne(reverseZone.getName(), nodeName)
        .map(dnsNode -> DnsPair.builder()
            .zoneName(reverseZone.getName())
            .node(dnsNode)
            .build())
        .orElse(DnsPair.builder()
            .zoneName(reverseZone.getName())
            .node(DnsNode.builder()
                .name(nodeName)
                .build())
            .build());
  }

  @Override
  public Optional<DnsNode> save(@NotNull final String zoneName, @NotNull final DnsNode dnsNode) {

    // Collect deleted records and save existing dns node
    final Set<DnsRecord> deletedRecords = new LinkedHashSet<>();
    DnsNode newDnsNode = findOne(zoneName, dnsNode.getName())
        .map(existingDnsNode -> {
          for (final DnsRecord existingDnsRecord : existingDnsNode.getRecords()) {
            if (!dnsNode.getRecords().contains(existingDnsRecord)) {
              deletedRecords.add(existingDnsRecord);
            }
          }
          return getLdapTemplate().save(dnsNode, getDnsNodeLdapMapper(zoneName));
        })
        .orElse(DnsNode.builder()
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
        getLdapTemplate().delete(dnsNode, getDnsNodeLdapMapper(zoneName));
      }
      newDnsNode = null;
    } else {
      // Add new record via cli
      add(zoneName, dnsNode.getName(), newRecords);
      // Load dns node from ldap
      newDnsNode = findOne(zoneName, dnsNode.getName())
          .orElseThrow(() -> ServiceException.internalServerError("Saving dns node failed."));
    }

    // Do A record to PTR record synchronization
    handlePtrRecords(zoneName, dnsNode.getName(), newRecords, deletedRecords);

    return Optional.ofNullable(newDnsNode);
  }

  private void add(
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
  public boolean delete(@NotNull final String zoneName, @NotNull final DnsNode node) {
    getLdapTemplate().delete(node, getDnsNodeLdapMapper(zoneName));
    handlePtrRecords(zoneName, node.getName(), Collections.emptySet(), node.getRecords());
    return true;
  }

  private void handlePtrRecords(
      final String zoneName,
      final String nodeName,
      final Set<DnsRecord> newRecords,
      final Set<DnsRecord> deletedRecords) {

    if (dnsZoneRepository.isDnsReverseZone(zoneName)) {
      return;
    }
    for (final DnsRecord record : deletedRecords) {
      findReverseDnsNode(zoneName, record).ifPresent(pair -> {
        pair.getNode().getRecords().remove(DnsRecord.builder()
            .recordType(DnsRecordType.PTR.name())
            .recordValue(nodeName + "." + zoneName)
            .build());
        save(pair.getZoneName(), pair.getNode());
      });
    }
    for (final DnsRecord record : newRecords) {
      findReverseDnsNode(zoneName, record).ifPresent(pair -> {
        final DnsRecord newRecord = DnsRecord.builder()
            .recordType(DnsRecordType.PTR.name())
            .recordValue(nodeName + "." + zoneName)
            .build();
        if (!pair.getNode().getRecords().contains(newRecord)) {
          pair.getNode().getRecords().add(newRecord);
          save(pair.getZoneName(), pair.getNode());
        }
      });
    }
  }

  /**
   * Find dns zone by an IPv4.
   *
   * @param ip the IPv4
   * @return the dns zone or {@code empty}
   */
  private Optional<DnsZone> findDnsZoneByIp4(final String ip) {
    return dnsZoneRepository.findDnsReverseZones()
        .filter(dnsZone -> ip4MatchesDnsZone(ip, dnsZone.getName()))
        .findFirst();
  }

  /**
   * Checks whether the given IPv4 (e. g. {@code 192.168.1.123}) matches the given dns zone name (e.
   * g. {@code 1.168.192.in-addr.arpa}).
   *
   * @param ip          the IPv4 (e. g. {@code 192.168.1.123})
   * @param dnsZoneName the dns reverse zone name (e. g. {@code 1.168.192.in-addr.arpa})
   * @return {@code true} if the ip matches the dns reverse zone, otherwise {@code false}
   */
  private boolean ip4MatchesDnsZone(final String ip, final String dnsZoneName) {
    if (ip == null || dnsZoneName == null) {
      return false;
    }
    final String ipPart = dnsZoneName.substring(
        0,
        dnsZoneName.length() - getProperties().getReverseZoneSuffixIp4().length());
    final String[] ipParts = ipPart.split(Pattern.quote("."));
    final StringBuilder ipBuilder = new StringBuilder();
    for (int i = ipParts.length - 1; i >= 0; i--) {
      ipBuilder.append(ipParts[i]).append('.');
    }
    return ip.startsWith(ipBuilder.toString());
  }

  /**
   * Returns the dns reverse entry name.
   *
   * @param ip                 the IPv4 (e. g. {@code 192.168.1.123}
   * @param dnsReverseZoneName the dns reverse zone name (e. g. {@code 1.168.192.in-addr.arpa}
   * @return the dns reverse entry name (e. g. {@code 123}
   */
  private String getDnsNodeNameByIp4(final String ip, final String dnsReverseZoneName) {
    Assert.hasText(ip, "IP must not be null or empty.");
    Assert.hasText(dnsReverseZoneName, "Dns reverse zone name must not be null or empty.");
    final String ipPart = dnsReverseZoneName.substring(
        0,
        dnsReverseZoneName.length() - getProperties().getReverseZoneSuffixIp4().length());
    final String[] ipParts = ipPart.split(Pattern.quote("."));
    final StringBuilder ipBuilder = new StringBuilder();
    for (int i = ipParts.length - 1; i >= 0; i--) {
      ipBuilder.append(ipParts[i]).append('.');
    }
    final String ipPrefix = ipBuilder.toString();
    Assert.isTrue(ip.startsWith(ipPrefix),
        "IP [" + ip + "] must start with [" + ipPrefix + "].");
    return ip.substring(ipPrefix.length());
  }

}
