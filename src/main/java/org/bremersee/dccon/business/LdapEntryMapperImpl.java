/*
 * Copyright 2017 the original author or authors.
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

package org.bremersee.dccon.business;

import static org.bremersee.dccon.business.LdapEntryUtils.UF_ACCOUNT_DISABLED;
import static org.bremersee.dccon.business.LdapEntryUtils.getAttributeValue;
import static org.bremersee.dccon.business.LdapEntryUtils.whenTimeToOffsetDateTime;

import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.SambaDomainProperties;
import org.bremersee.dccon.model.Name;
import org.bremersee.dccon.model.SambaGroup;
import org.bremersee.dccon.model.SambaGroupItem;
import org.bremersee.dccon.model.SambaUser;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The default implementation of the ldap entry mapper.
 *
 * @author Christian Bremer
 */
@Component
@Slf4j
public class LdapEntryMapperImpl implements LdapEntryMapper {

  private static final String WHEN_CREATED = "whenCreated";

  private static final String WHEN_CHANGED = "whenChanged";

  private final SambaDomainProperties properties;

  /**
   * Instantiates a new ldap entry mapper.
   *
   * @param properties the properties
   */
  @Autowired
  public LdapEntryMapperImpl(final SambaDomainProperties properties) {
    this.properties = properties;
  }

  private void mapLdapEntryToGroupItem(
      @NotNull final LdapEntry ldapEntry,
      @NotNull final SambaGroupItem group) {

    group.setDistinguishedName(ldapEntry.getDn());
    group.setCreated(whenTimeToOffsetDateTime(
        getAttributeValue(ldapEntry, WHEN_CREATED, null)));
    group.setModified(whenTimeToOffsetDateTime(
        getAttributeValue(ldapEntry, WHEN_CHANGED, null)));
    group.setName(getAttributeValue(ldapEntry, "sAMAccountName", null));
  }

  @Override
  public SambaGroupItem mapLdapEntryToSambaGroupItem(@NotNull final LdapEntry ldapEntry) {
    final SambaGroupItem group = new SambaGroupItem();
    mapLdapEntryToGroupItem(ldapEntry, group);
    return group;
  }

  @Override
  public SambaGroup mapLdapEntryToSambaGroup(@NotNull final LdapEntry ldapEntry) {
    final SambaGroup group = new SambaGroup();
    mapLdapEntryToGroupItem(ldapEntry, group);
    LdapAttribute membersAttr = ldapEntry.getAttribute(properties.getGroupMemberAttr());
    if (membersAttr != null && membersAttr.getStringValues() != null) {
      group.setMembers(membersAttr.getStringValues().stream().map(member -> {
        final Name name = new Name();
        name.setValue(member);
        name.setDistinguishedName(properties.isMemberDn());
        return name;
      }).collect(Collectors.toList()));
    }
    return group;
  }

  @Override
  public SambaUser mapLdapEntryToSambaUser(@NotNull final LdapEntry ldapEntry) {
    final SambaUser user = new SambaUser();
    user.setDistinguishedName(ldapEntry.getDn());
    user.setCreated(whenTimeToOffsetDateTime(
        getAttributeValue(ldapEntry, WHEN_CREATED, null)));
    user.setModified(whenTimeToOffsetDateTime(
        getAttributeValue(ldapEntry, WHEN_CHANGED, null)));
    user.setUserName(getAttributeValue(ldapEntry, "sAMAccountName", null));
    user.setDisplayName(
        getAttributeValue(ldapEntry, "displayName",
            getAttributeValue(ldapEntry, "gecos", null)));
    user.setEmail(getAttributeValue(ldapEntry, "mail", null));
    user.setMobile(getAttributeValue(ldapEntry, "telephoneNumber", null));
    LdapAttribute groupsAttr = ldapEntry.getAttribute(properties.getUserGroupAttr());
    if (groupsAttr != null && groupsAttr.getStringValues() != null) {
      user.setGroups(groupsAttr.getStringValues().stream().map(group -> {
        final Name name = new Name();
        name.setValue(group);
        name.setDistinguishedName(properties.isUserGroupDn());
        return name;
      }).collect(Collectors.toList()));
    }
    final int userAccountControl = LdapEntryUtils.getUserAccountControl(ldapEntry);
    user.setEnabled(((userAccountControl & UF_ACCOUNT_DISABLED) != UF_ACCOUNT_DISABLED));
    final String pwdLastSet = getAttributeValue(ldapEntry, "last", null);
    user.setPasswordLastSet(LdapEntryUtils.activeDirectoryTimeToOffsetDateTime(pwdLastSet));
    return user;
  }

}
