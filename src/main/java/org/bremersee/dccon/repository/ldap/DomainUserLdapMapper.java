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
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.repository.ldap.transcoder.ActiveDirectoryTimeValueTranscoder;
import org.bremersee.dccon.repository.ldap.transcoder.UserAccountControlValueTranscoder;
import org.bremersee.dccon.repository.ldap.transcoder.UserGroupValueTranscoder;
import org.ldaptive.AttributeModification;
import org.ldaptive.LdapEntry;
import org.ldaptive.io.ByteArrayValueTranscoder;
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

  private static ByteArrayValueTranscoder BYTE_ARRAY_VALUE_TRANSCODER
      = new ByteArrayValueTranscoder();

  private static IntegerValueTranscoder INT_VALUE_TRANSCODER = new IntegerValueTranscoder();

  private static ActiveDirectoryTimeValueTranscoder AD_TIME_VALUE_TRANSCODER
      = new ActiveDirectoryTimeValueTranscoder();

  private static UserAccountControlValueTranscoder USER_ACCOUNT_CONTROL_VALUE_TRANSCODER
      = new UserAccountControlValueTranscoder();

  private UserGroupValueTranscoder userGroupValueTranscoder;

  /**
   * Instantiates a new domain user ldap mapper.
   *
   * @param properties the properties
   */
  public DomainUserLdapMapper(DomainControllerProperties properties) {
    super(properties);
    this.userGroupValueTranscoder = new UserGroupValueTranscoder(properties);
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
    domainUser.setUserName(getAttributeValue(ldapEntry,
        "sAMAccountName", STRING_VALUE_TRANSCODER, null));
    domainUser.setFirstName(getAttributeValue(ldapEntry,
        "givenName", STRING_VALUE_TRANSCODER, null));
    domainUser.setLastName(getAttributeValue(ldapEntry,
        "sn", STRING_VALUE_TRANSCODER, null));
    domainUser.setDisplayName(getAttributeValue(ldapEntry,
        "displayName", STRING_VALUE_TRANSCODER, getAttributeValue(ldapEntry,
            "gecos", STRING_VALUE_TRANSCODER, null)));
    domainUser.setEmail(getAttributeValue(ldapEntry,
        "mail", STRING_VALUE_TRANSCODER, null));
    domainUser.setTelephoneNumber(getAttributeValue(ldapEntry,
        "telephoneNumber", STRING_VALUE_TRANSCODER, null));
    domainUser.setMobile(getAttributeValue(ldapEntry,
        "mobile", STRING_VALUE_TRANSCODER, null));
    domainUser.setGroups(LdaptiveEntryMapper.getAttributeValuesAsList(ldapEntry,
        getProperties().getUserGroupAttr(), userGroupValueTranscoder));
    domainUser
        .setDescription(getAttributeValue(ldapEntry, "description", STRING_VALUE_TRANSCODER, null));

    domainUser.setHomeDirectory(getAttributeValue(ldapEntry,
        "homeDirectory", STRING_VALUE_TRANSCODER, null));
    domainUser.setUnixHomeDirectory(getAttributeValue(ldapEntry,
        "unixHomeDirectory", STRING_VALUE_TRANSCODER, null));
    domainUser.setLoginShell(getAttributeValue(ldapEntry,
        "loginShell", STRING_VALUE_TRANSCODER, null));

    domainUser.setLastLogon(getAttributeValue(ldapEntry,
        "lastLogon", AD_TIME_VALUE_TRANSCODER, null));
    domainUser.setLogonCount(getAttributeValue(ldapEntry,
        "logonCount", INT_VALUE_TRANSCODER, null));
    domainUser.setPasswordLastSet(getAttributeValue(ldapEntry,
        "pwdLastSet", AD_TIME_VALUE_TRANSCODER, null));
    domainUser.setAvatar(getAttributeValue(ldapEntry,
        "jpegPhoto", BYTE_ARRAY_VALUE_TRANSCODER, null));

    Integer userAccountControlValue = getAttributeValue(ldapEntry,
        "userAccountControl", USER_ACCOUNT_CONTROL_VALUE_TRANSCODER, null);
    domainUser.setEnabled(
        UserAccountControlValueTranscoder.isUserAccountEnabled(userAccountControlValue));
  }

  @Override
  public AttributeModification[] mapAndComputeModifications(
      final DomainUser source,
      final LdapEntry destination) {

    final List<AttributeModification> modifications = new ArrayList<>();
    setAttribute(destination,
        "sAMAccountName", source.getUserName(), false, STRING_VALUE_TRANSCODER,
        modifications);
    setAttribute(destination,
        "name", source.getUserName(), false, STRING_VALUE_TRANSCODER,
        modifications);
    setAttribute(destination,
        "uid", source.getUserName(), false, STRING_VALUE_TRANSCODER,
        modifications);
    setAttribute(destination,
        "givenName", source.getFirstName(), false, STRING_VALUE_TRANSCODER, modifications);
    setAttribute(destination,
        "sn", source.getLastName(), false, STRING_VALUE_TRANSCODER, modifications);
    setAttribute(destination,
        "displayName", source.getDisplayName(), false, STRING_VALUE_TRANSCODER,
        modifications);
    setAttribute(destination,
        "gecos", source.getDisplayName(), false, STRING_VALUE_TRANSCODER,
        modifications);
    setAttribute(destination,
        "mail", source.getEmail(), false, STRING_VALUE_TRANSCODER, modifications);
    setAttribute(destination,
        "telephoneNumber", source.getTelephoneNumber(), false, STRING_VALUE_TRANSCODER,
        modifications);
    setAttribute(destination,
        "mobile", source.getMobile(), false, STRING_VALUE_TRANSCODER, modifications);
    setAttribute(destination,
        "description", source.getDescription(), false, STRING_VALUE_TRANSCODER, modifications);

    setAttribute(destination,
        "homeDirectory", source.getHomeDirectory(), false, STRING_VALUE_TRANSCODER,
        modifications);
    setAttribute(destination,
        "unixHomeDirectory", source.getUnixHomeDirectory(), false, STRING_VALUE_TRANSCODER,
        modifications);
    setAttribute(destination,
        "loginShell", source.getLoginShell(), false, STRING_VALUE_TRANSCODER,
        modifications);
    setAttribute(destination,
        "jpegPhoto", source.getAvatar(), true, BYTE_ARRAY_VALUE_TRANSCODER, modifications);

    setAttributes(destination,
        getProperties().getUserGroupAttr(), source.getGroups(), false, userGroupValueTranscoder,
        modifications);

    Integer userAccountControlValue = getAttributeValue(destination,
        "userAccountControl", USER_ACCOUNT_CONTROL_VALUE_TRANSCODER, null);
    userAccountControlValue = UserAccountControlValueTranscoder.getUserAccountControlValue(
        source.getEnabled(),
        userAccountControlValue);
    setAttribute(destination,
        "userAccountControl", userAccountControlValue, false,
        USER_ACCOUNT_CONTROL_VALUE_TRANSCODER, modifications);

    return modifications.toArray(new AttributeModification[0]);
  }

}
