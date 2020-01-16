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

package org.bremersee.dccon.repository.ldap;

import static org.bremersee.data.ldaptive.LdaptiveEntryMapper.getAttributeValue;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.bremersee.data.ldaptive.LdaptiveEntryMapper;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DnsNode;
import org.bremersee.dccon.model.DnsRecord;
import org.bremersee.dccon.model.UnknownFilter;
import org.bremersee.dccon.repository.ldap.transcoder.DnsRecordValueTranscoder;
import org.ldaptive.AttributeModification;
import org.ldaptive.LdapEntry;
import org.ldaptive.io.StringValueTranscoder;

/**
 * The dns node ldap mapper.
 *
 * @author Christian Bremer
 */
public class DnsNodeLdapMapper extends AbstractLdapMapper implements LdaptiveEntryMapper<DnsNode> {

  private static final StringValueTranscoder STRING_VALUE_TRANSCODER = new StringValueTranscoder();

  private static final DnsRecordValueTranscoder DNS_RECORD_VALUE_TRANSCODER
      = new DnsRecordValueTranscoder();

  private final String zoneName;

  private final UnknownFilter unknownFilter;

  /**
   * Instantiates a new dns node ldap mapper.
   *
   * @param properties the properties
   * @param zoneName the zone name
   * @param unknownFilter the unknown filter
   */
  public DnsNodeLdapMapper(
      DomainControllerProperties properties,
      String zoneName,
      UnknownFilter unknownFilter) {
    super(properties);
    this.zoneName = zoneName;
    this.unknownFilter = unknownFilter != null ? unknownFilter : UnknownFilter.NO_UNKNOWN;
  }

  @Override
  public String[] getObjectClasses() {
    return new String[0];
  }

  @Override
  public String mapDn(final DnsNode dnsNode) {
    return createDn(
        getProperties().getDnsNodeRdn(),
        dnsNode.getName(),
        getProperties().buildDnsNodeBaseDn(zoneName));
  }

  @Override
  public DnsNode map(final LdapEntry ldapEntry) {
    if (ldapEntry == null) {
      return null;
    }
    final DnsNode destination = new DnsNode();
    map(ldapEntry, destination);
    if (destination.getRecords().isEmpty() && unknownFilter != UnknownFilter.ALL) {
      return null;
    }
    return destination;
  }

  @Override
  public void map(
      final LdapEntry ldapEntry,
      final DnsNode dnsNode) {
    if (ldapEntry == null) {
      return;
    }
    mapCommonAttributes(ldapEntry, dnsNode);
    dnsNode.setName(getAttributeValue(ldapEntry, "name", STRING_VALUE_TRANSCODER, null));
    dnsNode.setRecords(LdaptiveEntryMapper
        .getAttributeValuesAsSet(ldapEntry, "dnsRecord", DNS_RECORD_VALUE_TRANSCODER)
        .stream()
        .filter(unknownFilter::matches)
        .collect(Collectors.toSet()));
    dnsNode.setRecords(new TreeSet<>(dnsNode.getRecords()));
  }

  @Override
  public AttributeModification[] mapAndComputeModifications(
      final DnsNode source,
      final LdapEntry destination) {

    // Because I cannot transcode the record value into the raw record value,
    // this call keeps existing records and deletes non existing records.
    // Adding new records has to be done via the command line interface
    // (see save method in DnsNodeRepositoryImpl).
    final List<AttributeModification> modifications = new ArrayList<>();
    final List<DnsRecord> delete = new ArrayList<>();
    final DnsNode destinationNode = map(destination);
    if (destinationNode != null) {
      for (final DnsRecord record : destinationNode.getRecords()) {
        if (!source.getRecords().contains(record)) {
          delete.add(record);
        }
      }
    }
    LdaptiveEntryMapper.removeAttributes(
        destination, "dnsRecord", delete, DNS_RECORD_VALUE_TRANSCODER, modifications);
    return modifications.toArray(new AttributeModification[0]);
  }

}
