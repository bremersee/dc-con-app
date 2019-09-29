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

import static org.bremersee.dccon.model.UnknownFilter.ALL;
import static org.bremersee.dccon.model.UnknownFilter.NO_UNKNOWN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.data.ldaptive.LdaptiveEntryMapper;
import org.bremersee.data.ldaptive.LdaptiveTemplate;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DhcpLease;
import org.bremersee.dccon.model.DnsNode;
import org.bremersee.dccon.model.DnsPair;
import org.bremersee.dccon.model.DnsRecord;
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.model.UnknownFilter;
import org.bremersee.dccon.repository.cli.CommandExecutor;
import org.bremersee.dccon.repository.ldap.DnsNodeLdapMapper;
import org.bremersee.exception.ServiceException;
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.Nullable;
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
public class DnsNodeRepositoryImpl extends AbstractRepository implements DnsNodeRepository {

  private final Executor asyncExecutor;

  private final DhcpRepository dhcpRepository;

  private final DnsZoneRepository dnsZoneRepository;

  private final Map<String, LdaptiveEntryMapper<DnsNode>> dnsNodeLdapMapperMap;

  private final List<Pattern> excludedNodeNamePatterns;

  private final Pattern patternIp4;

  private DnsNodeLdapMapperProvider dnsNodeLdapMapperProvider;

  /**
   * Instantiates a new dns node repository.
   *
   * @param properties        the properties
   * @param ldapTemplate      the ldap template
   * @param asyncExecutor     the async executor
   * @param dhcpRepository    the dhcp repository
   * @param dnsZoneRepository the dns zone repository
   */
  public DnsNodeRepositoryImpl(
      final DomainControllerProperties properties,
      final LdaptiveTemplate ldapTemplate,
      final Executor asyncExecutor,
      final DhcpRepository dhcpRepository,
      final DnsZoneRepository dnsZoneRepository) {
    super(properties, ldapTemplate);
    this.asyncExecutor = asyncExecutor;
    this.dhcpRepository = dhcpRepository;
    this.dnsZoneRepository = dnsZoneRepository;
    this.dnsNodeLdapMapperMap = new ConcurrentHashMap<>();
    this.dnsNodeLdapMapperProvider = (zoneName, unknownFilter) -> new DnsNodeLdapMapper(
        getProperties(), zoneName, unknownFilter);
    this.excludedNodeNamePatterns = properties.getExcludedNodeRegexList().stream()
        .map(Pattern::compile).collect(Collectors.toList());
    this.patternIp4 = Pattern.compile(properties.getIp4Regex());
  }

  private void createDhcpLeaseCache(final String zoneName) {
    asyncExecutor.execute(() -> {
      final boolean isReverseZone = dnsZoneRepository.isDnsReverseZone(zoneName);
      if (isReverseZone) {
        dhcpRepository.findActiveByHostName();
      } else {
        dhcpRepository.findActiveByIp();
      }
    });
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

  private boolean isNonExcludedDnsNode(final DnsNode dnsNode) {
    return dnsNode != null && !isExcludedDnsNode(dnsNode);
  }

  private boolean isNonExcludedDnsNode(final String dnsNodeName) {
    return dnsNodeName != null && !isExcludedDnsNode(dnsNodeName);
  }

  private boolean isExcludedDnsNode(final DnsNode dnsNode) {
    return dnsNode != null && isExcludedDnsNode(dnsNode.getName());
  }

  private boolean isExcludedDnsNode(final String dnsNodeName) {
    return dnsNodeName != null && excludedNodeNamePatterns.stream()
        .anyMatch(pattern -> pattern.matcher(dnsNodeName).matches());
  }

  @Override
  public List<DnsNode> findByIps(final Set<String> ips, final UnknownFilter unknownFilter) {
    final List<DnsNode> nodes = new ArrayList<>();
    for (DnsZone zone : dnsZoneRepository.findNonDnsReverseZones().collect(Collectors.toList())) {
      for (DnsNode node : findAll(zone.getName(), unknownFilter).collect(Collectors.toList())) {
        for (DnsRecord record : node.getRecords()) {
          if (DnsRecordType.A.is(record.getRecordType())
              && ips.contains(record.getRecordValue())) {
            nodes.add(node);
          }
        }
      }
    }
    return nodes;
  }

  @Override
  public Optional<DnsNode> findByHostName(
      final String hostName,
      final UnknownFilter unknownFilter) {

    final List<String> zoneNames = dnsZoneRepository.findNonDnsReverseZones()
        .map(DnsZone::getName)
        .sorted((o1, o2) -> {
          int n1 = StringUtils.countOccurrencesOf(o1, ".");
          int n2 = StringUtils.countOccurrencesOf(o2, ".");
          return n2 - n1;
        })
        .collect(Collectors.toList());
    for (String zoneName : zoneNames) {
      final String suffix = "." + zoneName;
      if (hostName.endsWith(suffix)) {
        final String name = hostName.substring(0, hostName.length() - suffix.length());
        return findOne(zoneName, name, unknownFilter);
      }
    }
    return findOne(getProperties().getDefaultZone(), hostName, unknownFilter);
  }

  @Override
  public Stream<DnsNode> findAll(final String zoneName, final UnknownFilter unknownFilter) {

    createDhcpLeaseCache(zoneName);
    final SearchRequest searchRequest = new SearchRequest(
        getProperties().buildDnsNodeBaseDn(zoneName),
        new SearchFilter(getProperties().getDnsNodeFindAllFilter()));
    searchRequest.setSearchScope(getProperties().getDnsNodeFindAllSearchScope());
    searchRequest.setBinaryAttributes("dnsRecord");
    return getLdapTemplate().findAll(searchRequest, getDnsNodeLdapMapper(zoneName, unknownFilter))
        .filter(this::isNonExcludedDnsNode)
        .map(dnsNode -> insertCorrelationValues(zoneName, dnsNode))
        .map(dnsNode -> insertDhcpLeases(zoneName, dnsNode));
  }

  @Override
  public boolean exists(
      final String zoneName,
      final String nodeName,
      final UnknownFilter unknownFilter) {
    return isNonExcludedDnsNode(nodeName)
        && dnsZoneRepository.exists(zoneName)
        && getLdapTemplate().exists(DnsNode.builder().name(nodeName).build(),
        getDnsNodeLdapMapper(zoneName, unknownFilter));
  }

  @Override
  public Optional<DnsNode> findOne(
      final String zoneName,
      final String nodeName,
      final UnknownFilter unknownFilter) {
    return findOne(zoneName, nodeName, unknownFilter, true, true);
  }

  private Optional<DnsNode> findOne(
      final String zoneName,
      final String nodeName,
      final UnknownFilter unknownFilter,
      final boolean withCorrelationValues,
      final boolean withDhcpLeases) {

    createDhcpLeaseCache(zoneName);
    final SearchFilter searchFilter = new SearchFilter(getProperties().getDnsNodeFindOneFilter());
    searchFilter.setParameter(0, nodeName);
    final SearchRequest searchRequest = new SearchRequest(
        getProperties().buildDnsNodeBaseDn(zoneName),
        searchFilter);
    searchRequest.setSearchScope(getProperties().getDnsNodeFindAllSearchScope());
    searchRequest.setBinaryAttributes("dnsRecord");
    return getLdapTemplate().findOne(searchRequest, getDnsNodeLdapMapper(zoneName, unknownFilter))
        .filter(this::isNonExcludedDnsNode)
        .map(dnsNode -> withCorrelationValues
            ? insertCorrelationValues(zoneName, dnsNode)
            : dnsNode)
        .map(dnsNode -> withDhcpLeases
            ? insertDhcpLeases(zoneName, dnsNode)
            : dnsNode);
  }

  private DnsNode insertDhcpLeases(
      final String zoneName,
      final DnsNode dnsNode) {

    final boolean isReverseZone = dnsZoneRepository.isDnsReverseZone(zoneName);
    final Map<String, DhcpLease> leaseMap = isReverseZone
        ? dhcpRepository.findActiveByHostName() : dhcpRepository.findActiveByIp();

    for (final DnsRecord record : dnsNode.getRecords()) {
      if (DnsRecordType.A.is(record.getRecordType())) {
        record.setDhcpLease(leaseMap.get(record.getRecordValue()));
      } else if (DnsRecordType.PTR.is(record.getRecordType())
          && record.getRecordValue().endsWith("." + getProperties().getDefaultZone())) {
        final String hostName = record.getRecordValue().substring(
            0,
            record.getRecordValue().length() - ("." + getProperties().getDefaultZone()).length())
            .toLowerCase();
        record.setDhcpLease(leaseMap.get(hostName));
      }
    }
    return dnsNode;
  }

  private DnsNode insertCorrelationValues(
      final String zoneName,
      final DnsNode dnsNode) {
    dnsNode.setRecords(dnsNode.getRecords().stream()
        .map(dnsRecord -> insertCorrelationValue(zoneName, dnsRecord))
        .collect(Collectors.toSet()));
    return dnsNode;
  }

  private DnsRecord insertCorrelationValue(
      final String zoneName,
      final DnsRecord dnsRecord) {
    return findCorrelatedDnsNode(zoneName, dnsRecord)
        .filter(dnsPair -> Boolean.TRUE.equals(dnsPair.getNodeExists()))
        .flatMap(dnsPair -> dnsPair.getNode().getRecords().stream()
            .filter(correlatedRecord -> DnsRecordType.areCorrelated(dnsRecord, correlatedRecord))
            .findAny())
        .map(correlatedRecord -> {
          dnsRecord.setCorrelatedRecordValue(correlatedRecord.getRecordValue());
          return dnsRecord;
        })
        .orElse(dnsRecord);
  }

  @Override
  public Optional<DnsPair> findCorrelatedDnsNode(
      final String zoneName,
      final DnsRecord record) {

    if (DnsRecordType.A.is(record.getRecordType())) {
      final String ip4 = record.getRecordValue();
      return findDnsZoneByIp4(ip4)
          .flatMap(reverseZone -> buildDnsPairByIp4(ip4, reverseZone));
    } else if (DnsRecordType.PTR.is(record.getRecordType())) {
      final String fqdn = record.getRecordValue();
      return findDnsZoneByFqdn(fqdn)
          .flatMap(zone -> buildDnsPairByFqdn(fqdn, zone));
    }
    return Optional.empty();
  }

  private Optional<DnsPair> buildDnsPairByIp4(
      final String ip4,
      final DnsZone reverseZone) {

    return getDnsNodeNameByIp4(ip4, reverseZone.getName())
        .map(nodeName -> findOne(reverseZone.getName(), nodeName, NO_UNKNOWN, false, false)
            .map(dnsNode -> DnsPair.builder()
                .zoneName(reverseZone.getName())
                .node(dnsNode)
                .nodeExists(true)
                .build())
            .orElseGet(() -> DnsPair.builder()
                .zoneName(reverseZone.getName())
                .node(DnsNode.builder()
                    .name(nodeName)
                    .build())
                .nodeExists(false)
                .build()));
  }

  private Optional<DnsPair> buildDnsPairByFqdn(final String fqdn, final DnsZone dnsZone) {
    return getDnsNodeNameByFqdn(fqdn, dnsZone.getName())
        .map(nodeName -> findOne(dnsZone.getName(), nodeName, NO_UNKNOWN, false, false)
            .map(dnsNode -> DnsPair.builder()
                .zoneName(dnsZone.getName())
                .node(dnsNode)
                .nodeExists(true)
                .build())
            .orElseGet(() -> DnsPair.builder()
                .zoneName(dnsZone.getName())
                .node(DnsNode.builder()
                    .name(nodeName)
                    .build())
                .nodeExists(false)
                .build()));
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
            getLdapTemplate().delete(
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
        getLdapTemplate().delete(dnsNode, getDnsNodeLdapMapper(zoneName, ALL));
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
  public boolean delete(@NotNull String zoneName, @NotNull String nodeName) {
    return findOne(zoneName, nodeName, ALL, false, false)
        .map(node -> delete(zoneName, node))
        .orElse(false);
  }

  @Override
  public boolean delete(final String zoneName, final DnsNode node) {
    if (isExcludedDnsNode(node)) {
      throw ServiceException.badRequest(
          "Node name is not allowed.",
          "org.bremersee:dc-con-app:3e377240-eafd-45ea-9ee6-048ab3ca8cec");
    }
    getLdapTemplate().delete(node, getDnsNodeLdapMapper(zoneName, ALL));
    handlePtrRecords(zoneName, node.getName(), Collections.emptySet(), node.getRecords());
    return true;
  }

  @Override
  public void deleteAll(@NotNull String zoneName, @Nullable Collection<String> nodeNames) {
    if (nodeNames != null && !nodeNames.isEmpty()) {
      for (String nodeName : new LinkedHashSet<>(nodeNames)) {
        findOne(zoneName, nodeName, ALL, false, false)
            .ifPresent(dnsNode -> delete(zoneName, dnsNode));
      }
    }
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
      findCorrelatedDnsNode(zoneName, record).ifPresent(pair -> {
        pair.getNode().getRecords().remove(DnsRecord.builder()
            .recordType(DnsRecordType.PTR.name())
            .recordValue(nodeName + "." + zoneName)
            .build());
        save(pair.getZoneName(), pair.getNode());
      });
    }
    for (final DnsRecord record : newRecords) {
      findCorrelatedDnsNode(zoneName, record).ifPresent(pair -> {
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

  private Optional<DnsZone> findDnsZoneByFqdn(final String fqdn) {
    final Map<String, DnsZone> zoneMap = dnsZoneRepository.findNonDnsReverseZones()
        .collect(Collectors.toMap(dnsNode -> dnsNode.getName().toLowerCase(), dnsNode -> dnsNode));
    String tmp = fqdn;
    int i;
    while ((i = tmp.indexOf('.')) > -1) {
      final String zoneName = fqdn.substring(i + 1);
      final DnsZone dnsZone = zoneMap.get(zoneName.toLowerCase());
      if (dnsZone != null) {
        return Optional.of(dnsZone);
      }
      tmp = zoneName;
    }
    return Optional.empty();
  }

  /**
   * Checks whether the given IPv4 (e. g. {@code 192.168.1.123}) matches the given dns zone name (e.
   * g. {@code 1.168.192.in-addr.arpa}).
   *
   * @param ip       the IPv4 (e. g. {@code 192.168.1.123})
   * @param zoneName the dns reverse zone name (e. g. {@code 1.168.192.in-addr.arpa})
   * @return {@code true} if the ip matches the dns reverse zone, otherwise {@code false}
   */
  boolean ip4MatchesDnsZone(final String ip, final String zoneName) {
    if (ip == null || zoneName == null || !patternIp4.matcher(ip).matches()) {
      return false;
    }
    return Optional.ofNullable(splitIp4(ip, zoneName))
        .map(ipParts -> ip.equals(ipParts[0] + "." + ipParts[1]))
        .orElse(false);
  }

  /**
   * Returns the dns reverse node name.
   *
   * @param ip       the IPv4 (e. g. {@code 192.168.1.123}
   * @param zoneName the dns reverse zone name (e. g. {@code 1.168.192.in-addr.arpa}
   * @return the dns reverse node name (e. g. {@code 123}
   */
  Optional<String> getDnsNodeNameByIp4(final String ip, final String zoneName) {
    return Optional.ofNullable(splitIp4(ip, zoneName)).map(parts -> parts[1]);
  }

  /**
   * Returns the dns node name.
   *
   * @param fqdn     the full qualified domain name (e. g. {@code pluto.eixe.bremersee.org})
   * @param zoneName the dns zone name (e. g. {@code eixe.bremersee.org})
   * @return the dns node name (e. g. {@code pluto})
   */
  Optional<String> getDnsNodeNameByFqdn(String fqdn, String zoneName) {
    if (fqdn == null || zoneName == null) {
      return Optional.empty();
    }
    if (fqdn.toLowerCase().endsWith("." + zoneName.toLowerCase())) {
      return Optional.of(fqdn.substring(0, fqdn.length() - ("." + zoneName).length()));
    }
    return Optional.empty();
  }

  /**
   * Split Ipv4 into parts, e. g. {@code 192.168.1} from the dns reverse zone name and into the node
   * name {@code 123}.
   *
   * @param ip       the IPv4 (e. g. {@code 192.168.1.123}
   * @param zoneName the dns reverse zone name (e. g. {@code 1.168.192.in-addr.arpa}
   * @return the Ipv4 parts or {@code null} if the ip doesn't belong to the dns reverse zone
   */
  String[] splitIp4(final String ip, final String zoneName) {
    if (ip == null || zoneName == null
        || zoneName.length() <= getProperties().getReverseZoneSuffixIp4().length()
        || !patternIp4.matcher(ip).matches()) {
      return null;
    }
    final String ipPart = zoneName.substring(
        0,
        zoneName.length() - getProperties().getReverseZoneSuffixIp4().length());
    final String[] ipParts = ipPart.split(Pattern.quote("."));
    final StringBuilder ipBuilder = new StringBuilder();
    for (int i = ipParts.length - 1; i >= 0; i--) {
      ipBuilder.append(ipParts[i]);
      if (i > 0) {
        ipBuilder.append('.');
      }
    }
    final String ipPrefix = ipBuilder.toString();
    final String ipPostfix = ip.substring(ipPrefix.length() + 1);
    // ipPrefix is something like 192.168.1
    // ipPostfix is something like 123
    // or
    // ipPrefix is something like 192.168
    // ipPostfix is something like 1.123 // TODO is this correct or do it have to be 123.1 ?
    if (!ip.equals(ipPrefix + "." + ipPostfix)) {
      return null;
    }
    return new String[]{ipPrefix, ipPostfix};
  }

}
