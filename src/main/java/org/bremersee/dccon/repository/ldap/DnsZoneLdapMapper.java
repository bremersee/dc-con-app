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

import org.bremersee.data.ldaptive.LdaptiveEntryMapper;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DnsZone;
import org.ldaptive.AttributeModification;
import org.ldaptive.LdapEntry;
import org.ldaptive.io.StringValueTranscoder;

/**
 * The dns zone ldap mapper.
 *
 * @author Christian Bremer
 */
public class DnsZoneLdapMapper extends AbstractLdapMapper implements LdaptiveEntryMapper<DnsZone> {

  private static final StringValueTranscoder STRING_VALUE_TRANSCODER = new StringValueTranscoder();

  /**
   * Instantiates a new dns zone ldap mapper.
   *
   * @param properties the properties
   */
  public DnsZoneLdapMapper(DomainControllerProperties properties) {
    super(properties);
  }

  @Override
  public String[] getObjectClasses() {
    return new String[0];
  }

  @Override
  public String mapDn(final DnsZone dnsZone) {
    return createDn(
        getProperties().getDnsZoneRdn(),
        dnsZone.getName(),
        getProperties().getDnsZoneBaseDn());
  }

  @Override
  public DnsZone map(final LdapEntry ldapEntry) {
    if (ldapEntry == null) {
      return null;
    }
    final DnsZone destination = new DnsZone();
    map(ldapEntry, destination);
    return destination;
  }

  @Override
  public void map(
      final LdapEntry ldapEntry,
      final DnsZone dnsZone) {
    if (ldapEntry == null) {
      return;
    }
    mapCommonAttributes(ldapEntry, dnsZone);
    dnsZone.setName(getAttributeValue(ldapEntry, "name", STRING_VALUE_TRANSCODER, null));
  }

  @Override
  public AttributeModification[] mapAndComputeModifications(
      final DnsZone source,
      final LdapEntry destination) {
    return new AttributeModification[0];
  }

}
