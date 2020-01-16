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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import org.bremersee.data.ldaptive.LdaptiveTemplate;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DhcpLease;
import org.bremersee.dccon.model.DnsNode;
import org.bremersee.dccon.model.DnsPair;
import org.bremersee.dccon.model.DnsRecord;
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.model.UnknownFilter;
import org.springframework.util.StringUtils;

/**
 * The abstract dns node repository.
 *
 * @author Christian Bremer
 */
public abstract class AbstractDnsNodeRepository extends AbstractRepository
    implements DnsNodeRepository {

  @Getter(AccessLevel.PACKAGE)
  private final DhcpRepository dhcpRepository;

  @Getter(AccessLevel.PACKAGE)
  private final DnsZoneRepository dnsZoneRepository;

  @Getter(AccessLevel.PACKAGE)
  private final List<Pattern> excludedNodeNamePatterns;

  @Getter(AccessLevel.PACKAGE)
  private final Pattern patternIp4;

  /**
   * Instantiates a new abstract repository.
   *
   * @param properties        the properties
   * @param ldapTemplate      the ldap template
   * @param dhcpRepository    the dhcp repository
   * @param dnsZoneRepository the dns zone repository
   */
  AbstractDnsNodeRepository(
      final DomainControllerProperties properties,
      final LdaptiveTemplate ldapTemplate,
      final DhcpRepository dhcpRepository,
      final DnsZoneRepository dnsZoneRepository) {

    super(properties, ldapTemplate);
    this.dhcpRepository = dhcpRepository;
    this.dnsZoneRepository = dnsZoneRepository;
    this.excludedNodeNamePatterns = properties.getExcludedNodeRegexList().stream()
        .map(Pattern::compile)
        .collect(Collectors.toList());
    this.patternIp4 = Pattern.compile(properties.getIp4Regex());
  }

  /**
   * Is non excluded dns node boolean.
   *
   * @param dnsNode the dns node
   * @return the boolean
   */
  boolean isNonExcludedDnsNode(final DnsNode dnsNode) {
    return dnsNode != null && !isExcludedDnsNode(dnsNode);
  }

  /**
   * Is non excluded dns node boolean.
   *
   * @param dnsNodeName the dns node name
   * @return the boolean
   */
  boolean isNonExcludedDnsNode(final String dnsNodeName) {
    return dnsNodeName != null && !isExcludedDnsNode(dnsNodeName);
  }

  /**
   * Is excluded dns node boolean.
   *
   * @param dnsNode the dns node
   * @return the boolean
   */
  boolean isExcludedDnsNode(final DnsNode dnsNode) {
    return dnsNode != null && isExcludedDnsNode(dnsNode.getName());
  }

  /**
   * Is excluded dns node boolean.
   *
   * @param dnsNodeName the dns node name
   * @return the boolean
   */
  boolean isExcludedDnsNode(final String dnsNodeName) {
    return dnsNodeName != null && excludedNodeNamePatterns.stream()
        .anyMatch(pattern -> pattern.matcher(dnsNodeName).matches());
  }

  @Override
  public List<DnsNode> findByIps(final Set<String> ips, final UnknownFilter unknownFilter) {
    final List<DnsNode> nodes = new ArrayList<>();
    for (DnsZone zone : dnsZoneRepository.findNonDnsReverseZones().collect(Collectors.toList())) {
      for (DnsNode node : findAll(zone.getName(), unknownFilter, null)
          .collect(Collectors.toList())) {
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

  /**
   * Is query result boolean.
   *
   * @param dnsNode the dns node
   * @param query   the query
   * @return the boolean
   */
  boolean isQueryResult(final DnsNode dnsNode, final String query) {
    return query != null && query.length() > 2 && dnsNode != null
        && (contains(dnsNode.getName(), query)
        || isQueryResult(dnsNode.getRecords(), query));
  }

  /**
   * Is query result boolean.
   *
   * @param dnsRecords the dns records
   * @param query      the query
   * @return the boolean
   */
  boolean isQueryResult(final Collection<DnsRecord> dnsRecords, final String query) {
    if (dnsRecords != null) {
      for (DnsRecord dnsRecord : dnsRecords) {
        if (isQueryResult(dnsRecord, query)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Is query result boolean.
   *
   * @param dnsRecord the dns record
   * @param query     the query
   * @return the boolean
   */
  boolean isQueryResult(final DnsRecord dnsRecord, final String query) {
    return query != null && query.length() > 2 && dnsRecord != null
        && (contains(dnsRecord.getRecordValue(), query)
        || contains(dnsRecord.getCorrelatedRecordValue(), query)
        || isQueryResult(dnsRecord.getDhcpLease(), query));
  }

  /**
   * Is query result boolean.
   *
   * @param dhcpLease the dhcp lease
   * @param query     the query
   * @return the boolean
   */
  boolean isQueryResult(final DhcpLease dhcpLease, final String query) {
    return query != null && query.length() > 2 && dhcpLease != null
        && (contains(dhcpLease.getHostname(), query)
        || contains(dhcpLease.getIp(), query)
        || contains(dhcpLease.getMac(), query)
        || contains(dhcpLease.getManufacturer(), query));
  }

  /**
   * Find one optional.
   *
   * @param zoneName              the zone name
   * @param nodeName              the node name
   * @param unknownFilter         the unknown filter
   * @param withCorrelationValues the with correlation values
   * @param withDhcpLeases        the with dhcp leases
   * @return the optional
   */
  abstract Optional<DnsNode> findOne(
      String zoneName,
      String nodeName,
      UnknownFilter unknownFilter,
      boolean withCorrelationValues,
      boolean withDhcpLeases);

  @Override
  public Optional<DnsNode> findOne(
      final String zoneName,
      final String nodeName,
      final UnknownFilter unknownFilter) {
    return findOne(zoneName, nodeName, unknownFilter, true, true);
  }

  /**
   * Insert dhcp leases dns node.
   *
   * @param zoneName the zone name
   * @param dnsNode  the dns node
   * @return the dns node
   */
  DnsNode insertDhcpLeases(
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

  /**
   * Insert correlation values dns node.
   *
   * @param zoneName the zone name
   * @param dnsNode  the dns node
   * @return the dns node
   */
  DnsNode insertCorrelationValues(
      final String zoneName,
      final DnsNode dnsNode) {
    dnsNode.setRecords(dnsNode.getRecords().stream()
        .map(dnsRecord -> insertCorrelationValue(zoneName, dnsRecord))
        .collect(Collectors.toSet()));
    return dnsNode;
  }

  /**
   * Insert correlation value dns record.
   *
   * @param zoneName  the zone name
   * @param dnsRecord the dns record
   * @return the dns record
   */
  DnsRecord insertCorrelationValue(
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

  /**
   * Build dns pair by ip 4 optional.
   *
   * @param ip4         the ip 4
   * @param reverseZone the reverse zone
   * @return the optional
   */
  Optional<DnsPair> buildDnsPairByIp4(
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

  /**
   * Build dns pair by fqdn optional.
   *
   * @param fqdn    the fqdn
   * @param dnsZone the dns zone
   * @return the optional
   */
  Optional<DnsPair> buildDnsPairByFqdn(final String fqdn, final DnsZone dnsZone) {
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
  public boolean delete(final String zoneName, final String nodeName) {
    return findOne(zoneName, nodeName, ALL, false, false)
        .map(node -> delete(zoneName, node))
        .orElse(false);
  }

  @Override
  public void deleteAll(final String zoneName, final Collection<String> nodeNames) {
    if (nodeNames != null && !nodeNames.isEmpty()) {
      for (String nodeName : new LinkedHashSet<>(nodeNames)) {
        findOne(zoneName, nodeName, ALL, false, false)
            .ifPresent(dnsNode -> delete(zoneName, dnsNode));
      }
    }
  }

  /**
   * Handle ptr records.
   *
   * @param zoneName       the zone name
   * @param nodeName       the node name
   * @param newRecords     the new records
   * @param deletedRecords the deleted records
   */
  void handlePtrRecords(
      final String zoneName,
      final String nodeName,
      final Set<DnsRecord> newRecords,
      final Set<DnsRecord> deletedRecords) {

    if (dnsZoneRepository.isDnsReverseZone(zoneName)) {
      return;
    }
    for (final DnsRecord record : deletedRecords) {
      findCorrelatedDnsNode(zoneName, record).ifPresent(pair -> {
        final Set<DnsRecord> records = new LinkedHashSet<>(pair.getNode().getRecords());
        records.remove(DnsRecord.builder()
            .recordType(DnsRecordType.PTR.name())
            .recordValue(nodeName + "." + zoneName)
            .build());
        final DnsNode node = pair.getNode().toBuilder()
            .records(records)
            .build();
        save(pair.getZoneName(), node);
      });
    }
    for (final DnsRecord record : newRecords) {
      findCorrelatedDnsNode(zoneName, record).ifPresent(pair -> {
        final DnsRecord newRecord = DnsRecord.builder()
            .recordType(DnsRecordType.PTR.name())
            .recordValue(nodeName + "." + zoneName)
            .build();
        if (!pair.getNode().getRecords().contains(newRecord)) {
          final Set<DnsRecord> records = new LinkedHashSet<>(pair.getNode().getRecords());
          records.add(newRecord);
          final DnsNode node = pair.getNode().toBuilder()
              .records(records)
              .build();
          save(pair.getZoneName(), node);
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
  Optional<DnsZone> findDnsZoneByIp4(final String ip) {
    return dnsZoneRepository.findDnsReverseZones()
        .filter(dnsZone -> ip4MatchesDnsZone(ip, dnsZone.getName()))
        .findFirst();
  }

  /**
   * Find dns zone by fqdn optional.
   *
   * @param fqdn the fqdn
   * @return the optional
   */
  Optional<DnsZone> findDnsZoneByFqdn(final String fqdn) {
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
  Optional<String> getDnsNodeNameByFqdn(final String fqdn, final String zoneName) {
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
