/*
 * Copyright 2024 the original author or authors.
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
import static org.bremersee.ldaptive.LdaptiveEntryMapper.setAttribute;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.OrganizationalUnit;
import org.bremersee.ldaptive.LdaptiveEntryMapper;
import org.ldaptive.AttributeModification;
import org.ldaptive.LdapEntry;
import org.springframework.util.Assert;

/**
 * The organizational unit ldap mapper.
 *
 * @author Christian Bremer
 */
public class OrganizationalUnitLdapMapper extends AbstractLdapMapper
    implements LdaptiveEntryMapper<OrganizationalUnit> {

  public OrganizationalUnitLdapMapper(DomainControllerProperties properties) {
    super(properties);
  }

  @Override
  public String[] getObjectClasses() {
    return new String[0];
  }

  @Override
  public String mapDn(OrganizationalUnit organizationalUnit) {
    Assert.hasText(organizationalUnit.getDistinguishedName(),
        "DN of organizational unit is required.");
    return organizationalUnit.getDistinguishedName();
  }

  @Override
  public OrganizationalUnit map(LdapEntry ldapEntry) {
    if (ldapEntry == null) {
      return null;
    }
    OrganizationalUnit destination = new OrganizationalUnit();
    map(ldapEntry, destination);
    return destination;
  }

  @Override
  public void map(LdapEntry source, OrganizationalUnit destination) {

    if (isEmpty(source)) {
      return;
    }
    mapCommonAttributes(source, destination);

    destination.setDescription(
        getAttributeValue(source, LDAP_DESCRIPTION, STRING_VALUE_TRANSCODER, null));
    destination.setSystemOu(
        getAttributeValue(source, LDAP_IS_CRITICAL_SYSTEM_OBJECT, BOOLEAN_VALUE_TRANSCODER, false));
    destination.setName(
        getAttributeValue(source, LDAP_NAME, STRING_VALUE_TRANSCODER, null));
  }

  @Override
  public AttributeModification[] mapAndComputeModifications(
      OrganizationalUnit source,
      LdapEntry destination) {

    List<AttributeModification> modifications = new ArrayList<>();

    setAttribute(destination, LDAP_DESCRIPTION, source.getDescription(), false,
        STRING_VALUE_TRANSCODER, modifications);
    boolean isSystemOu = getAttributeValue(destination, LDAP_IS_CRITICAL_SYSTEM_OBJECT,
        BOOLEAN_VALUE_TRANSCODER, false);
    if (!isSystemOu) {
      setAttribute(destination, LDAP_NAME, source.getName(), false, STRING_VALUE_TRANSCODER,
          modifications);
    }

    return modifications.toArray(new AttributeModification[0]);
  }

}
