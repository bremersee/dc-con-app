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
import org.bremersee.data.ldaptive.LdaptiveEntryMapper;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DnsNode;
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

  private String zoneName;

  /**
   * Instantiates a new dns node ldap mapper.
   *
   * @param properties the properties
   * @param zoneName   the zone name
   */
  public DnsNodeLdapMapper(
      DomainControllerProperties properties,
      String zoneName) {
    super(properties);
    this.zoneName = zoneName;
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
    final DnsNode destination = new DnsNode();
    map(ldapEntry, destination);
    return destination;
  }

  @Override
  public AttributeModification[] mapAndComputeModifications(
      final DnsNode source,
      final LdapEntry destination) {

    // Because I cannot transcode the record value into the raw record value,
    // this call won't change the record.
    // Changes have to be done in the repository implementation.
    final List<AttributeModification> modifications = new ArrayList<>();
    LdaptiveEntryMapper.setAttributes(destination,
        "dnsRecord", source.getRecords(), true, DNS_RECORD_VALUE_TRANSCODER, modifications);
    return modifications.toArray(new AttributeModification[0]);
  }

  @Override
  public void map(
      final LdapEntry ldapEntry,
      final DnsNode dnsNode) {
    mapCommonAttributes(ldapEntry, dnsNode);
    dnsNode.setName(getAttributeValue(ldapEntry, "name", STRING_VALUE_TRANSCODER, null));
    dnsNode.setRecords(LdaptiveEntryMapper
        .getAttributeValuesAsSet(ldapEntry, "dnsRecord", DNS_RECORD_VALUE_TRANSCODER));
  }
}
