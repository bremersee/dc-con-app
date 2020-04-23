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
import static org.bremersee.dccon.repository.ldap.DomainUserLdapConstants.DESCRIPTION;
import static org.bremersee.dccon.repository.ldap.DomainUserLdapConstants.DISPLAY_NAME;
import static org.bremersee.dccon.repository.ldap.DomainUserLdapConstants.GECOS;
import static org.bremersee.dccon.repository.ldap.DomainUserLdapConstants.GIVEN_NAME;
import static org.bremersee.dccon.repository.ldap.DomainUserLdapConstants.HOME_DIRECTORY;
import static org.bremersee.dccon.repository.ldap.DomainUserLdapConstants.LAST_LOGON;
import static org.bremersee.dccon.repository.ldap.DomainUserLdapConstants.LOGIN_SHELL;
import static org.bremersee.dccon.repository.ldap.DomainUserLdapConstants.LOGON_COUNT;
import static org.bremersee.dccon.repository.ldap.DomainUserLdapConstants.MAIL;
import static org.bremersee.dccon.repository.ldap.DomainUserLdapConstants.MEMBER_OF;
import static org.bremersee.dccon.repository.ldap.DomainUserLdapConstants.MOBILE;
import static org.bremersee.dccon.repository.ldap.DomainUserLdapConstants.NAME;
import static org.bremersee.dccon.repository.ldap.DomainUserLdapConstants.OBJECT_SID;
import static org.bremersee.dccon.repository.ldap.DomainUserLdapConstants.PWD_LAST_SET;
import static org.bremersee.dccon.repository.ldap.DomainUserLdapConstants.SAM_ACCOUNT_NAME;
import static org.bremersee.dccon.repository.ldap.DomainUserLdapConstants.SN;
import static org.bremersee.dccon.repository.ldap.DomainUserLdapConstants.TELEPHONE_NUMBER;
import static org.bremersee.dccon.repository.ldap.DomainUserLdapConstants.UID;
import static org.bremersee.dccon.repository.ldap.DomainUserLdapConstants.UNIX_HOME_DIRECTORY;
import static org.bremersee.dccon.repository.ldap.DomainUserLdapConstants.USER_ACCOUNT_CONTROL;

import java.util.ArrayList;
import java.util.List;
import org.bremersee.data.ldaptive.LdaptiveEntryMapper;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.repository.ldap.transcoder.FileTimeToOffsetDateTimeValueTranscoder;
import org.bremersee.dccon.repository.ldap.transcoder.SidValueTranscoder;
import org.bremersee.dccon.repository.ldap.transcoder.UserAccountControlValueTranscoder;
import org.bremersee.dccon.repository.ldap.transcoder.UserGroupValueTranscoder;
import org.ldaptive.AttributeModification;
import org.ldaptive.LdapEntry;
import org.ldaptive.io.IntegerValueTranscoder;
import org.ldaptive.io.StringValueTranscoder;

/**
 * The domain user ldap mapper.
 *
 * @author Christian Bremer
 */
public class DomainUserLdapMapper extends AbstractLdapMapper implements
    LdaptiveEntryMapper<DomainUser> {

  private static final StringValueTranscoder STRING_VALUE_TRANSCODER = new StringValueTranscoder();

  private static IntegerValueTranscoder INT_VALUE_TRANSCODER = new IntegerValueTranscoder();

  private static FileTimeToOffsetDateTimeValueTranscoder AD_TIME_VALUE_TRANSCODER
      = new FileTimeToOffsetDateTimeValueTranscoder();

  private static UserAccountControlValueTranscoder USER_ACCOUNT_CONTROL_VALUE_TRANSCODER
      = new UserAccountControlValueTranscoder();

  private UserGroupValueTranscoder userGroupValueTranscoder;

  private SidValueTranscoder sidValueTranscoder;

  /**
   * Instantiates a new domain user ldap mapper.
   *
   * @param properties the properties
   */
  public DomainUserLdapMapper(DomainControllerProperties properties) {
    super(properties);
    this.userGroupValueTranscoder = new UserGroupValueTranscoder(properties);
    this.sidValueTranscoder = new SidValueTranscoder(properties);
  }

  @Override
  public String[] getObjectClasses() {
    return new String[0];
  }

  @Override
  public String mapDn(final DomainUser domainUser) {
    return createDn(
        getProperties().getUserRdn(),
        domainUser.getUserName(),
        getProperties().getUserBaseDn());
  }

  @Override
  public DomainUser map(final LdapEntry ldapEntry) {
    if (ldapEntry == null) {
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
    if (ldapEntry == null) {
      return;
    }
    mapCommonAttributes(ldapEntry, domainUser);
    domainUser.setSid(getAttributeValue(ldapEntry,
        OBJECT_SID, sidValueTranscoder, null));
    domainUser.setUserName(getAttributeValue(ldapEntry,
        SAM_ACCOUNT_NAME, STRING_VALUE_TRANSCODER, null));
    domainUser.setFirstName(getAttributeValue(ldapEntry,
        GIVEN_NAME, STRING_VALUE_TRANSCODER, null));
    domainUser.setLastName(getAttributeValue(ldapEntry,
        SN, STRING_VALUE_TRANSCODER, null));
    domainUser.setDisplayName(getAttributeValue(ldapEntry,
        DISPLAY_NAME, STRING_VALUE_TRANSCODER, getAttributeValue(ldapEntry,
            GECOS, STRING_VALUE_TRANSCODER, null)));
    domainUser.setEmail(getAttributeValue(ldapEntry,
        MAIL, STRING_VALUE_TRANSCODER, null));
    domainUser.setTelephoneNumber(getAttributeValue(ldapEntry,
        TELEPHONE_NUMBER, STRING_VALUE_TRANSCODER, null));
    domainUser.setMobile(getAttributeValue(ldapEntry,
        MOBILE, STRING_VALUE_TRANSCODER, null));
    domainUser.setGroups(LdaptiveEntryMapper.getAttributeValuesAsList(ldapEntry,
        MEMBER_OF, userGroupValueTranscoder));
    domainUser.getGroups().sort(String::compareToIgnoreCase);
    domainUser.setDescription(getAttributeValue(ldapEntry,
        DESCRIPTION, STRING_VALUE_TRANSCODER, null));
    domainUser.setHomeDirectory(getAttributeValue(ldapEntry,
        HOME_DIRECTORY, STRING_VALUE_TRANSCODER, null));
    domainUser.setUnixHomeDirectory(getAttributeValue(ldapEntry,
        UNIX_HOME_DIRECTORY, STRING_VALUE_TRANSCODER, null));
    domainUser.setLoginShell(getAttributeValue(ldapEntry,
        LOGIN_SHELL, STRING_VALUE_TRANSCODER, null));
    domainUser.setLastLogon(getAttributeValue(ldapEntry,
        LAST_LOGON, AD_TIME_VALUE_TRANSCODER, null));
    domainUser.setLogonCount(getAttributeValue(ldapEntry,
        LOGON_COUNT, INT_VALUE_TRANSCODER, null));
    domainUser.setPasswordLastSet(getAttributeValue(ldapEntry,
        PWD_LAST_SET, AD_TIME_VALUE_TRANSCODER, null));

    Integer userAccountControlValue = getAttributeValue(ldapEntry,
        USER_ACCOUNT_CONTROL, USER_ACCOUNT_CONTROL_VALUE_TRANSCODER, null);
    domainUser.setEnabled(
        UserAccountControlValueTranscoder.isUserAccountEnabled(userAccountControlValue));
  }

  @Override
  public AttributeModification[] mapAndComputeModifications(
      final DomainUser source,
      final LdapEntry destination) {

    final List<AttributeModification> modifications = new ArrayList<>();
    setAttribute(destination,
        SAM_ACCOUNT_NAME, source.getUserName(), false, STRING_VALUE_TRANSCODER,
        modifications);
    setAttribute(destination,
        NAME, source.getUserName(), false, STRING_VALUE_TRANSCODER,
        modifications);
    setAttribute(destination,
        UID, source.getUserName(), false, STRING_VALUE_TRANSCODER,
        modifications);
    setAttribute(destination,
        GIVEN_NAME, source.getFirstName(), false, STRING_VALUE_TRANSCODER, modifications);
    setAttribute(destination,
        SN, source.getLastName(), false, STRING_VALUE_TRANSCODER, modifications);
    setAttribute(destination,
        DISPLAY_NAME, source.getDisplayName(), false, STRING_VALUE_TRANSCODER,
        modifications);
    setAttribute(destination,
        GECOS, source.getDisplayName(), false, STRING_VALUE_TRANSCODER,
        modifications);
    setAttribute(destination,
        MAIL, source.getEmail(), false, STRING_VALUE_TRANSCODER, modifications);
    setAttribute(destination,
        TELEPHONE_NUMBER, source.getTelephoneNumber(), false, STRING_VALUE_TRANSCODER,
        modifications);
    setAttribute(destination,
        MOBILE, source.getMobile(), false, STRING_VALUE_TRANSCODER, modifications);
    setAttribute(destination,
        DESCRIPTION, source.getDescription(), false, STRING_VALUE_TRANSCODER, modifications);

    setAttribute(destination,
        HOME_DIRECTORY, source.getHomeDirectory(), false, STRING_VALUE_TRANSCODER,
        modifications);
    setAttribute(destination,
        UNIX_HOME_DIRECTORY, source.getUnixHomeDirectory(), false, STRING_VALUE_TRANSCODER,
        modifications);
    setAttribute(destination,
        LOGIN_SHELL, source.getLoginShell(), false, STRING_VALUE_TRANSCODER,
        modifications);

    // Groups must be set in group entity.
    // setAttributes(destination,
    //     "memberOf", source.getGroups(), false, userGroupValueTranscoder,
    //     modifications);

    Integer userAccountControlValue = getAttributeValue(destination,
        USER_ACCOUNT_CONTROL, USER_ACCOUNT_CONTROL_VALUE_TRANSCODER, null);
    userAccountControlValue = UserAccountControlValueTranscoder.getUserAccountControlValue(
        source.getEnabled(),
        userAccountControlValue);
    setAttribute(destination,
        USER_ACCOUNT_CONTROL, userAccountControlValue, false,
        USER_ACCOUNT_CONTROL_VALUE_TRANSCODER, modifications);

    return modifications.toArray(new AttributeModification[0]);
  }

}
