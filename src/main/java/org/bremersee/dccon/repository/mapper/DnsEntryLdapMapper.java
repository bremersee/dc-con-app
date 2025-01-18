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

package org.bremersee.dccon.repository.mapper;

import static org.bremersee.ldaptive.LdaptiveEntryMapper.getAttributeValue;

import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DnsEntry;
import org.bremersee.ldaptive.LdaptiveEntryMapper;
import org.ldaptive.AttributeModification;
import org.ldaptive.LdapEntry;

/**
 * The dns node ldap mapper.
 *
 * @author Christian Bremer
 */
public class DnsEntryLdapMapper extends AbstractLdapMapper
    implements LdaptiveEntryMapper<DnsEntry> {

  public DnsEntryLdapMapper(DomainControllerProperties properties) {
    super(properties);
  }

  @Override
  public String[] getObjectClasses() {
    return new String[0];
  }

  @Override
  public String mapDn(final DnsEntry dnsEntry) {
    return dnsEntry.getDistinguishedName();
  }

  @Override
  public DnsEntry map(final LdapEntry ldapEntry) {
    if (ldapEntry == null) {
      return null;
    }
    final DnsEntry destination = new DnsEntry();
    map(ldapEntry, destination);
    return destination;
  }

  @Override
  public void map(
      final LdapEntry ldapEntry,
      final DnsEntry dnsEntry) {
    if (ldapEntry == null) {
      return;
    }
    CommonAttributesLdapMapper.mapCommonAttributes(ldapEntry, dnsEntry);
    dnsEntry.setName(getAttributeValue(ldapEntry, "name", STRING_VALUE_TRANSCODER, null));
  }

  @Override
  public AttributeModification[] mapAndComputeModifications(
      final DnsEntry source,
      final LdapEntry destination) {

    return new AttributeModification[0];
  }

}
