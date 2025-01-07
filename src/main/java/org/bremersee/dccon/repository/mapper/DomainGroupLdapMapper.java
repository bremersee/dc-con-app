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
import static org.bremersee.ldaptive.LdaptiveEntryMapper.getAttributeValuesAsList;
import static org.bremersee.ldaptive.LdaptiveEntryMapper.setAttribute;
import static org.bremersee.ldaptive.LdaptiveEntryMapper.setAttributes;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.model.DomainGroupTypeContainer;
import org.bremersee.dccon.repository.DomainGroupRepositoryConstants;
import org.bremersee.dccon.repository.DomainRepository;
import org.bremersee.ldaptive.LdaptiveEntryMapper;
import org.ldaptive.AttributeModification;
import org.ldaptive.LdapEntry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * The domain group ldap mapper.
 *
 * @author Christian Bremer
 */
@Component
@Slf4j
public class DomainGroupLdapMapper extends AbstractLdapMapper
    implements LdaptiveEntryMapper<DomainGroup>, DomainGroupRepositoryConstants {

  private final DomainRepository domainRepository;

  @Getter(AccessLevel.PROTECTED)
  private boolean rfc2307Enabled;

  /**
   * Instantiates a new domain group ldap mapper.
   *
   * @param properties the properties
   */
  public DomainGroupLdapMapper(
      DomainControllerProperties properties,
      DomainRepository domainRepository) {
    super(properties);
    this.domainRepository = domainRepository;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void init() {
    rfc2307Enabled = domainRepository.isRfc2307Enabled();
  }

  @Override
  public String[] getObjectClasses() {
    return new String[0];
  }

  @Override
  public String mapDn(DomainGroup domainGroup) {
    Assert.hasText(domainGroup.getDistinguishedName(), "DN of domain group is required.");
    return domainGroup.getDistinguishedName();
  }

  @Override
  public DomainGroup map(LdapEntry ldapEntry) {
    if (ldapEntry == null) {
      return null;
    }
    DomainGroup destination = new DomainGroup();
    map(ldapEntry, destination);
    return destination;
  }

  @Override
  public void map(LdapEntry ldapEntry, DomainGroup domainGroup) {

    if (ldapEntry == null) {
      return;
    }
    mapCommonAttributes(ldapEntry, domainGroup);

    Integer groupType = getAttributeValue(ldapEntry, LDAP_GROUP_TYPE, INT_VALUE_TRANSCODER, null);
    domainGroup.setGroupType(new DomainGroupTypeContainer(groupType));
    domainGroup.setDescription(
        getAttributeValue(ldapEntry, LDAP_DESCRIPTION, STRING_VALUE_TRANSCODER, null));
    domainGroup.setGidNumber(
        getAttributeValue(ldapEntry, LDAP_GID_NUMBER, INT_VALUE_TRANSCODER, null));
    domainGroup.setEmail(getAttributeValue(ldapEntry, LDAP_MAIL, STRING_VALUE_TRANSCODER, null));
    domainGroup.setMembers(
        getAttributeValuesAsList(ldapEntry, LDAP_GROUP_MEMBER, STRING_VALUE_TRANSCODER));
    domainGroup.setMembership(
        getAttributeValuesAsList(ldapEntry, LDAP_MEMBER_OF_GROUP, STRING_VALUE_TRANSCODER));
    domainGroup.setNisDomain(
        getAttributeValue(ldapEntry, LDAP_NIS_DOMAIN, STRING_VALUE_TRANSCODER, null));
    domainGroup.setSamAccountName(
        getAttributeValue(ldapEntry, LDAP_SAM_ACCOUNT_NAME, STRING_VALUE_TRANSCODER, null));
    domainGroup.setSid(getAttributeValue(ldapEntry, LDAP_OBJECT_SID, SID_VALUE_TRANSCODER, null));
  }

  @Override
  public AttributeModification[] mapAndComputeModifications(
      DomainGroup source,
      LdapEntry destination) {

    List<AttributeModification> modifications = new ArrayList<>();

    if (isEmpty(getAttributeValue(destination, LDAP_CN, STRING_VALUE_TRANSCODER, null))) {
      setAttribute(destination, LDAP_CN, source.getSamAccountName(), false, STRING_VALUE_TRANSCODER,
          modifications);
    }
    setAttribute(destination, LDAP_GROUP_TYPE, source.getGroupType().getGroupTypeValue(),
        false, INT_VALUE_TRANSCODER, modifications);
    setAttribute(destination, LDAP_DESCRIPTION, source.getDescription(), false,
        STRING_VALUE_TRANSCODER, modifications);
    if (isRfc2307Enabled()) {
      setAttribute(destination, LDAP_GID_NUMBER, source.getGidNumber(), false,
          INT_VALUE_TRANSCODER, modifications);
    }
    setAttribute(destination, LDAP_MAIL, source.getEmail(), false, STRING_VALUE_TRANSCODER,
        modifications);
    setAttributes(destination, LDAP_GROUP_MEMBER, source.getMembers(), false,
        STRING_VALUE_TRANSCODER, modifications);
    // Members must be set in domain group entity.
    //setAttributes(destination, MEMBER_OF, source.getMembership(), false, STRING_VALUE_TRANSCODER, modifications);
    if (isEmpty(
        getAttributeValue(destination, LDAP_NAME, STRING_VALUE_TRANSCODER, null))) {
      setAttribute(destination, LDAP_NAME, source.getSamAccountName(), false,
          STRING_VALUE_TRANSCODER, modifications);
    }
    if (isRfc2307Enabled()) {
      setAttribute(destination, LDAP_NIS_DOMAIN, source.getNisDomain(), false,
          STRING_VALUE_TRANSCODER, modifications);
      setAttribute(destination, LDAP_NIS_NAME, source.getSamAccountName(), false,
          STRING_VALUE_TRANSCODER, modifications);
    }
    if (isEmpty(
        getAttributeValue(destination, LDAP_SAM_ACCOUNT_NAME, STRING_VALUE_TRANSCODER, null))) {
      setAttribute(destination, LDAP_SAM_ACCOUNT_NAME, source.getSamAccountName(), false,
          STRING_VALUE_TRANSCODER, modifications);
    }

    return modifications.toArray(new AttributeModification[0]);
  }

}

