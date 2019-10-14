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
import static org.bremersee.data.ldaptive.LdaptiveEntryMapper.setAttribute;
import static org.bremersee.data.ldaptive.LdaptiveEntryMapper.setAttributes;

import java.util.ArrayList;
import java.util.List;
import org.bremersee.data.ldaptive.LdaptiveEntryMapper;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.repository.ldap.transcoder.GroupMemberValueTranscoder;
import org.ldaptive.AttributeModification;
import org.ldaptive.LdapEntry;
import org.ldaptive.io.StringValueTranscoder;

/**
 * The domain group ldap mapper.
 *
 * @author Christian Bremer
 */
public class DomainGroupLdapMapper extends AbstractLdapMapper
    implements LdaptiveEntryMapper<DomainGroup> {

  private static final StringValueTranscoder STRING_VALUE_TRANSCODER = new StringValueTranscoder();

  private GroupMemberValueTranscoder groupMemberValueTranscoder;

  /**
   * Instantiates a new domain group ldap mapper.
   *
   * @param properties the properties
   */
  public DomainGroupLdapMapper(DomainControllerProperties properties) {
    super(properties);
    this.groupMemberValueTranscoder = new GroupMemberValueTranscoder(properties);
  }

  @Override
  public String[] getObjectClasses() {
    return new String[0];
  }

  @Override
  public String mapDn(final DomainGroup domainGroup) {
    return createDn(
        getProperties().getGroupRdn(),
        domainGroup.getName(),
        getProperties().getGroupBaseDn());
  }

  @Override
  public DomainGroup map(final LdapEntry ldapEntry) {
    if (ldapEntry == null) {
      return null;
    }
    final DomainGroup destination = new DomainGroup();
    map(ldapEntry, destination);
    return destination;
  }

  @Override
  public void map(
      final LdapEntry ldapEntry,
      final DomainGroup domainGroup) {
    if (ldapEntry == null) {
      return;
    }
    mapCommonAttributes(ldapEntry, domainGroup);
    domainGroup.setName(getAttributeValue(ldapEntry, "name", STRING_VALUE_TRANSCODER, null));
    domainGroup
        .setDescription(getAttributeValue(ldapEntry, "description", STRING_VALUE_TRANSCODER, null));
    domainGroup.setMembers(LdaptiveEntryMapper.getAttributeValuesAsList(
        ldapEntry,
        getProperties().getGroupMemberAttr(),
        groupMemberValueTranscoder));
    domainGroup.getMembers().sort(String::compareToIgnoreCase);
  }

  @Override
  public AttributeModification[] mapAndComputeModifications(
      final DomainGroup source,
      final LdapEntry destination) {
    final List<AttributeModification> modifications = new ArrayList<>();
    setAttribute(destination,
        "name", source.getName(), false, STRING_VALUE_TRANSCODER, modifications);
    setAttribute(destination,
        "sAMAccountName", source.getName(), false, STRING_VALUE_TRANSCODER, modifications);
    setAttribute(destination,
        "description", source.getDescription(), false, STRING_VALUE_TRANSCODER, modifications);
    setAttributes(
        destination,
        getProperties().getGroupMemberAttr(),
        source.getMembers(),
        false,
        groupMemberValueTranscoder,
        modifications);
    return modifications.toArray(new AttributeModification[0]);
  }

}

