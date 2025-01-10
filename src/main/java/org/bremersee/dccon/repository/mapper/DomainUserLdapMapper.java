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
import static org.bremersee.ldaptive.LdaptiveEntryMapper.setAttribute;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.repository.DomainRepository;
import org.bremersee.dccon.repository.DomainUserRepositoryConstants;
import org.bremersee.dccon.repository.transcoder.FileTimeToOffsetDateTimeValueTranscoder;
import org.bremersee.dccon.repository.transcoder.UserAccountControl;
import org.bremersee.dccon.repository.transcoder.UserAccountControlValueTranscoder;
import org.bremersee.ldaptive.LdaptiveEntryMapper;
import org.ldaptive.AttributeModification;
import org.ldaptive.LdapEntry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * The domain user ldap mapper.
 *
 * @author Christian Bremer
 */
@Component
@Slf4j
public class DomainUserLdapMapper extends AbstractLdapMapper
    implements LdaptiveEntryMapper<DomainUser>, DomainUserRepositoryConstants {

  private static final FileTimeToOffsetDateTimeValueTranscoder AD_TIME_VALUE_TRANSCODER
      = new FileTimeToOffsetDateTimeValueTranscoder();

  private static final UserAccountControlValueTranscoder USER_ACCOUNT_CONTROL_VALUE_TRANSCODER
      = new UserAccountControlValueTranscoder();

  private final DomainRepository domainRepository;

  @Getter(AccessLevel.PROTECTED)
  private boolean rfc2307Enabled;

  /**
   * Instantiates a new domain user ldap mapper.
   *
   * @param properties the properties
   */
  public DomainUserLdapMapper(
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
  public String mapDn(final DomainUser domainUser) {
    Assert.hasText(domainUser.getDistinguishedName(), "DN of domain user is required.");
    return domainUser.getDistinguishedName();
  }

  @Override
  public DomainUser map(final LdapEntry ldapEntry) {
    if (isEmpty(ldapEntry)) {
      return null;
    }
    final DomainUser destination = new DomainUser();
    map(ldapEntry, destination);
    return destination;
  }

  @Override
  public void map(
      final LdapEntry ldapEntry,
      final DomainUser domainUser) {

    if (isEmpty(ldapEntry)) {
      return;
    }
    mapSamAccount(ldapEntry, domainUser);

    domainUser.setAccountExpires(getAttributeValue(ldapEntry,
        LDAP_USER_ACCOUNT_EXPIRES, AD_TIME_VALUE_TRANSCODER, null));
    domainUser.setCompany(getAttributeValue(ldapEntry,
        LDAP_USER_COMPANY, STRING_VALUE_TRANSCODER, null));
    domainUser.setDepartment(getAttributeValue(ldapEntry,
        LDAP_USER_DEPARTMENT, STRING_VALUE_TRANSCODER, null));
    domainUser.setDescription(getAttributeValue(ldapEntry,
        LDAP_DESCRIPTION, STRING_VALUE_TRANSCODER, null));
    domainUser.setDisplayName(
        getAttributeValue(ldapEntry, LDAP_USER_DISPLAY_NAME, STRING_VALUE_TRANSCODER, null));
    domainUser.setGecos(
        getAttributeValue(ldapEntry, LDAP_USER_GECOS, STRING_VALUE_TRANSCODER, null));
    domainUser.setGidNumber(
        getAttributeValue(ldapEntry, LDAP_GID_NUMBER, INT_VALUE_TRANSCODER, null));
    domainUser.setFirstName(
        getAttributeValue(ldapEntry, LDAP_USER_GIVEN_NAME, STRING_VALUE_TRANSCODER, null));
    domainUser.setHomeDirectory(
        getAttributeValue(ldapEntry, LDAP_USER_HOME_DIRECTORY, STRING_VALUE_TRANSCODER, null));
    domainUser.setHomeDrive(
        getAttributeValue(ldapEntry, LDAP_USER_HOME_DRIVE, STRING_VALUE_TRANSCODER, null));
    domainUser.setInitials(
        getAttributeValue(ldapEntry, LDAP_USER_INITIALS, STRING_VALUE_TRANSCODER, null));
    domainUser.setLastLogon(
        getAttributeValue(ldapEntry, LDAP_USER_LAST_LOGON, AD_TIME_VALUE_TRANSCODER, null));
    domainUser.setLoginShell(
        getAttributeValue(ldapEntry, LDAP_USER_LOGIN_SHELL, STRING_VALUE_TRANSCODER, null));
    domainUser.setLogonCount(
        getAttributeValue(ldapEntry, LDAP_USER_LOGON_COUNT, INT_VALUE_TRANSCODER, null));
    domainUser.setEmail(getAttributeValue(ldapEntry, LDAP_MAIL, STRING_VALUE_TRANSCODER, null));
    domainUser.setMobile(
        getAttributeValue(ldapEntry, LDAP_USER_MOBILE, STRING_VALUE_TRANSCODER, null));
    domainUser.setNisDomain(
        getAttributeValue(ldapEntry, LDAP_NIS_DOMAIN, STRING_VALUE_TRANSCODER, null));
    domainUser.setPhysicalDeliveryOfficeName(
        getAttributeValue(ldapEntry, LDAP_USER_OFFICE_NAME, STRING_VALUE_TRANSCODER, null));
    domainUser.setPreferredLanguage(
        getAttributeValue(ldapEntry, LDAP_USER_PREFERRED_LANGUAGE, STRING_VALUE_TRANSCODER, null));
    domainUser.setProfilePath(
        getAttributeValue(ldapEntry, LDAP_USER_PROFILE_PATH, STRING_VALUE_TRANSCODER, null));
    domainUser.setPasswordLastSet(
        getAttributeValue(ldapEntry, LDAP_USER_PWD_LAST_SET, AD_TIME_VALUE_TRANSCODER, null));
    domainUser.setScriptPath(
        getAttributeValue(ldapEntry, LDAP_USER_SCRIPT_PATH, STRING_VALUE_TRANSCODER, null));
    domainUser.setLastName(
        getAttributeValue(ldapEntry, LDAP_USER_SN, STRING_VALUE_TRANSCODER, null));
    domainUser.setTelephoneNumber(
        getAttributeValue(ldapEntry, LDAP_USER_TELEPHONE_NUMBER, STRING_VALUE_TRANSCODER, null));
    domainUser.setTitle(
        getAttributeValue(ldapEntry, LDAP_USER_TITLE, STRING_VALUE_TRANSCODER, null));
    String uid = getAttributeValue(ldapEntry, LDAP_USER_UID, STRING_VALUE_TRANSCODER, null);
    domainUser.setUid(
        getAttributeValue(ldapEntry, LDAP_NIS_NAME, STRING_VALUE_TRANSCODER, uid));
    domainUser.setUidNumber(
        getAttributeValue(ldapEntry, LDAP_USER_UID_NUMBER, INT_VALUE_TRANSCODER, null));
    domainUser.setUnixHomeDirectory(
        getAttributeValue(ldapEntry, LDAP_USER_UNIX_HOME_DIRECTORY, STRING_VALUE_TRANSCODER, null));
    domainUser.setUserPrincipalName(
        getAttributeValue(ldapEntry, LDAP_USER_USER_PRINCIPAL_NAME, STRING_VALUE_TRANSCODER, null));
    domainUser.setAccountControl(
        getAttributeValue(ldapEntry, LDAP_USER_USER_ACCOUNT_CONTROL,
            USER_ACCOUNT_CONTROL_VALUE_TRANSCODER, null).toDomainUserAccountControl());
  }

  @Override
  public AttributeModification[] mapAndComputeModifications(
      final DomainUser source,
      final LdapEntry destination) {

    final List<AttributeModification> modifications = new ArrayList<>();

    mapSamAccount(source, destination, modifications);

    setAttribute(destination, LDAP_USER_COMPANY, source.getCompany(), false,
        STRING_VALUE_TRANSCODER, modifications);
    setAttribute(destination, LDAP_USER_DEPARTMENT, source.getDepartment(), false,
        STRING_VALUE_TRANSCODER, modifications);
    setAttribute(destination, LDAP_DESCRIPTION, source.getDescription(), false,
        STRING_VALUE_TRANSCODER, modifications);
    String displayName = source.getDisplayName();
    if (isEmpty(displayName) && !isEmpty(source.getFirstName()) && !isEmpty(source.getLastName())) {
      displayName = source.getFirstName() + " " + source.getLastName();
    }
    setAttribute(destination, LDAP_USER_DISPLAY_NAME, displayName, false,
        STRING_VALUE_TRANSCODER, modifications);
    if (isRfc2307Enabled()) {
      String gecos = source.getGecos();
      if (isEmpty(gecos) && !isEmpty(source.getFirstName()) && !isEmpty(source.getLastName())) {
        gecos = source.getFirstName() + " " + source.getLastName();
      }
      setAttribute(destination, LDAP_USER_GECOS, gecos, false,
          STRING_VALUE_TRANSCODER, modifications);
    }
    if (isRfc2307Enabled()) {
      setAttribute(destination, LDAP_GID_NUMBER, source.getGidNumber(), false,
          INT_VALUE_TRANSCODER, modifications);
    }
    setAttribute(destination, LDAP_USER_GIVEN_NAME, source.getFirstName(), false,
        STRING_VALUE_TRANSCODER, modifications);
    setAttribute(destination, LDAP_USER_HOME_DIRECTORY, source.getHomeDirectory(), false,
        STRING_VALUE_TRANSCODER, modifications);
    setAttribute(destination, LDAP_USER_HOME_DRIVE, source.getHomeDrive(), false,
        STRING_VALUE_TRANSCODER, modifications);
    setAttribute(destination, LDAP_USER_INITIALS, source.getInitials(), false,
        STRING_VALUE_TRANSCODER, modifications);
    // last login is read only
    if (isRfc2307Enabled()) {
      setAttribute(destination, LDAP_USER_LOGIN_SHELL, source.getLoginShell(), false,
          STRING_VALUE_TRANSCODER, modifications);
    }
    // logon count is read only
    setAttribute(destination, LDAP_MAIL, source.getEmail(), false, STRING_VALUE_TRANSCODER,
        modifications);
    // Members must be set in domain group entity.
    // setAttributes(destination, MEMBER_OF, source.getMemberships(), false, userGroupValueTranscoder, modifications);
    setAttribute(destination, LDAP_USER_MOBILE, source.getMobile(), false, STRING_VALUE_TRANSCODER,
        modifications);
    // NOT_ALLOWED_ON_RDN, diagnosticMessage=00002016: Modify of 'name' not permitted, must use 'rename' operation instead
    // setAttribute(destination, LDAP_NAME, getName(source), false, STRING_VALUE_TRANSCODER, modifications);
    if (isRfc2307Enabled()) {
      setAttribute(destination, LDAP_NIS_DOMAIN, source.getNisDomain(), false,
          STRING_VALUE_TRANSCODER, modifications);
      setAttribute(destination, LDAP_NIS_NAME, source.getUid(), false,
          STRING_VALUE_TRANSCODER, modifications);
    }
    // sid is read only
    setAttribute(destination, LDAP_USER_OFFICE_NAME, source.getPhysicalDeliveryOfficeName(),
        false, STRING_VALUE_TRANSCODER, modifications);
    setAttribute(destination, LDAP_USER_PREFERRED_LANGUAGE, source.getPreferredLanguage(), false,
        STRING_VALUE_TRANSCODER, modifications);
    setAttribute(destination, LDAP_USER_PROFILE_PATH, source.getProfilePath(), false,
        STRING_VALUE_TRANSCODER, modifications);
    setAttribute(destination, LDAP_USER_SCRIPT_PATH, source.getScriptPath(), false,
        STRING_VALUE_TRANSCODER, modifications);
    setAttribute(destination, LDAP_USER_SN, source.getLastName(), false, STRING_VALUE_TRANSCODER,
        modifications);
    setAttribute(destination, LDAP_USER_TELEPHONE_NUMBER, source.getTelephoneNumber(), false,
        STRING_VALUE_TRANSCODER, modifications);
    setAttribute(destination, LDAP_USER_TITLE, source.getTitle(), false, STRING_VALUE_TRANSCODER,
        modifications);
    if (isRfc2307Enabled()) {
      setAttribute(destination, LDAP_USER_UID, source.getUid(), false,
          STRING_VALUE_TRANSCODER, modifications);
      setAttribute(destination, LDAP_USER_UID_NUMBER, source.getUidNumber(), false,
          INT_VALUE_TRANSCODER, modifications);
      setAttribute(destination, LDAP_USER_UNIX_HOME_DIRECTORY, source.getUnixHomeDirectory(), false,
          STRING_VALUE_TRANSCODER, modifications);
    }
    Integer userAccountControlValue = getAttributeValue(destination, LDAP_USER_USER_ACCOUNT_CONTROL,
        INT_VALUE_TRANSCODER, new UserAccountControl().getValue());
    UserAccountControl userAccountControl = new UserAccountControl(
        userAccountControlValue, source.getAccountControl());
    setAttribute(destination, LDAP_USER_USER_ACCOUNT_CONTROL, userAccountControl, false,
        USER_ACCOUNT_CONTROL_VALUE_TRANSCODER, modifications);
    if (!isEmpty(source.getUserPrincipalName())) {
      setAttribute(destination, LDAP_USER_USER_PRINCIPAL_NAME, source.getUserPrincipalName(), false,
          STRING_VALUE_TRANSCODER, modifications);
    }

    return modifications.toArray(new AttributeModification[0]);
  }

  private String getName(DomainUser domainUser) {
    if (isEmpty(domainUser)) {
      return null;
    }
    if (!isEmpty(domainUser.getDisplayName())) {
      return domainUser.getDisplayName();
    }
    return domainUser.getSamAccountName();
  }

}
