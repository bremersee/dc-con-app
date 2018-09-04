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

package org.bremersee.smbcon.business;

import static org.bremersee.common.exhandling.ServiceException.internalServerError;
import static org.bremersee.smbcon.business.LdapEntryUtils.UF_ACCOUNT_DISABLED;
import static org.bremersee.smbcon.business.LdapEntryUtils.UF_DONT_EXPIRE_PASSWD;
import static org.bremersee.smbcon.business.LdapEntryUtils.UF_NORMAL_ACCOUNT;
import static org.bremersee.smbcon.business.LdapEntryUtils.createDn;
import static org.bremersee.smbcon.business.LdapEntryUtils.getAttributeValue;
import static org.bremersee.smbcon.business.LdapEntryUtils.getUserAccountControl;
import static org.bremersee.smbcon.business.LdapEntryUtils.updateAttribute;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.common.exhandling.ServiceException;
import org.bremersee.smbcon.SambaDomainProperties;
import org.bremersee.smbcon.exception.AlreadyExistsException;
import org.bremersee.smbcon.exception.NotFoundException;
import org.bremersee.smbcon.model.DnsEntry;
import org.bremersee.smbcon.model.DnsRecordType;
import org.bremersee.smbcon.model.DnsZone;
import org.bremersee.smbcon.model.Name;
import org.bremersee.smbcon.model.Names;
import org.bremersee.smbcon.model.Password;
import org.bremersee.smbcon.model.SambaGroup;
import org.bremersee.smbcon.model.SambaGroupItem;
import org.bremersee.smbcon.model.SambaUser;
import org.bremersee.smbcon.model.SambaUserAddRequest;
import org.ldaptive.AttributeModification;
import org.ldaptive.AttributeModificationType;
import org.ldaptive.Connection;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.ModifyOperation;
import org.ldaptive.ModifyRequest;
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchOperation;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The sSamba connector service implementation.
 *
 * @author Christian Bremer
 */
@Component
@Slf4j
public class SambaConnectorServiceImpl implements SambaConnectorService {

  private final SambaDomainProperties properties;

  private final LdapEntryMapper mapper;

  private final ConnectionFactory connectionFactory;

  private final SambaTool sambaTool;

  /**
   * Instantiates a new Samba connector service.
   *
   * @param properties        the properties
   * @param mapper            the mapper
   * @param connectionFactory the connection factory
   * @param sambaTool         the samba tool
   */
  @Autowired
  public SambaConnectorServiceImpl(
      final SambaDomainProperties properties,
      final LdapEntryMapper mapper,
      final ConnectionFactory connectionFactory,
      final SambaTool sambaTool) {

    this.properties = properties;
    this.mapper = mapper;
    this.connectionFactory = connectionFactory;
    this.sambaTool = sambaTool;
  }

  @Override
  public List<SambaGroupItem> getGroups() {

    log.info("msg=[Getting samba groups]");
    final SearchFilter sf = new SearchFilter(properties.getGroupFindAllFilter());
    final SearchRequest sr = new SearchRequest(properties.getGroupBaseDn(), sf);
    sr.setSearchScope(properties.getGroupFindAllSearchScope());
    Connection conn = null;
    try {
      conn = getConnection();
      final SearchOperation so = new SearchOperation(conn);
      final SearchResult searchResult = so.execute(sr).getResult();
      final List<SambaGroupItem> groups = searchResult.getEntries()
          .stream()
          .map(mapper::mapLdapEntryToSambaGroupItem)
          .collect(Collectors.toList());
      log.info("msg=[Getting samba groups] resultSize=[{}]", groups.size());
      return groups;

    } catch (final LdapException e) {
      final ServiceException se = internalServerError(
          "Getting samba groups failed.",
          e);
      log.error("msg=[Getting samba groups failed.]", se);
      throw se;

    } finally {
      closeConnection(conn);
    }
  }

  @Override
  public SambaGroup addGroup(@Valid final SambaGroup group) {

    log.info("msg=[Adding samba group] group=[{}]", group);
    try {
      getGroupByName(group.getName());
      throw new AlreadyExistsException(group);

    } catch (final NotFoundException nfe) {

      sambaTool.addGroup(group.getName());
      final Names names = new Names();
      names.setValues(group.getMembers());
      return updateGroupMembers(group.getName(), names);
    }
  }

  private Optional<LdapEntry> findGroupByName(
      @NotNull final String groupName,
      @NotNull final Connection conn) throws LdapException {

    final SearchFilter sf = new SearchFilter(properties.getGroupFindOneFilter());
    sf.setParameters(new Object[]{groupName});
    final SearchRequest sr = new SearchRequest(properties.getGroupBaseDn(), sf);
    sr.setSearchScope(properties.getGroupFindOneSearchScope());
    final SearchOperation so = new SearchOperation(conn);
    final SearchResult searchResult = so.execute(sr).getResult();
    final LdapEntry ldapEntry = searchResult.getEntry();
    return ldapEntry == null ? Optional.empty() : Optional.of(ldapEntry);
  }

  @Override
  public SambaGroup getGroupByName(@NotNull final String groupName) {

    log.info("msg=[Getting samba group by name] name=[{}]", groupName);
    Connection conn = null;
    try {
      conn = getConnection();
      final SambaGroup group = mapper.mapLdapEntryToSambaGroup(
          findGroupByName(groupName, conn).orElseThrow(NotFoundException::new));
      log.info("msg=[Getting samba group by name] name=[{}] group=[{}]", groupName, group);
      return group;

    } catch (final LdapException e) {
      final ServiceException se = internalServerError(
          "Getting samba group by name failed.",
          e);
      log.error("msg=[Getting samba group by name failed.]", se);
      throw se;

    } finally {
      closeConnection(conn);
    }
  }

  @Override
  public SambaGroup updateGroupMembers(
      @NotNull final String groupName,
      @Valid final Names members) {

    log.info("msg=[Updating samba group members] group=[{}] members=[{}]", groupName, members);
    Connection conn = null;
    try {
      conn = getConnection();
      LdapEntry ldapEntry = findGroupByName(groupName, conn).orElseThrow(NotFoundException::new);

      final LdapAttribute memberAttr = ldapEntry.getAttribute(properties.getGroupMemberAttr());
      final boolean hasMemberAttr = memberAttr != null;
      final Set<String> oldUserDns = hasMemberAttr
          ? new HashSet<>(memberAttr.getStringValues())
          : new HashSet<>();
      log.debug("msg=[Updating members of group] group=[{}] oldMembers=[{}]",
          groupName, oldUserDns);

      final Set<String> newUserDns = members.getValues()
          .stream()
          .map(name -> name.isDistinguishedName()
              ? name.getValue()
              : createDn(properties.getUserRdn(), name.getValue(), properties.getUserBaseDn()))
          .collect(Collectors.toSet());
      log.debug("msg=[Updating members of group] group=[{}] newMembers=[{}]",
          groupName, newUserDns);

      final Set<String> both = new HashSet<>(oldUserDns);
      both.retainAll(newUserDns);
      log.debug("msg=[Updating members of group] group=[{}] keptMembers=[{}]", groupName, both);

      oldUserDns.removeAll(both);
      log.debug("msg=[Updating members of group] group=[{}] removedOldMembers=[{}]",
          groupName, oldUserDns);
      if (hasMemberAttr) {
        memberAttr.removeStringValues(oldUserDns);
      }

      newUserDns.removeAll(both);
      log.debug("msg=[Updating members of group] group=[{}] addedNewMembers=[{}]",
          groupName, newUserDns);
      if (hasMemberAttr) {
        memberAttr.addStringValues(newUserDns);
      } else {
        ldapEntry.addAttribute(new LdapAttribute(
            properties.getGroupMemberAttr(),
            newUserDns.toArray(new String[0])));
      }

      final List<AttributeModification> mods = new ArrayList<>();
      for (final String userDn : oldUserDns) {
        final LdapAttribute remAttr = new LdapAttribute(properties.getGroupMemberAttr(), userDn);
        mods.add(new AttributeModification(AttributeModificationType.REMOVE, remAttr));
      }

      for (final String userDn : newUserDns) {
        final LdapAttribute addAttr = new LdapAttribute(properties.getGroupMemberAttr(), userDn);
        mods.add(new AttributeModification(AttributeModificationType.ADD, addAttr));
      }

      if (!mods.isEmpty()) {
        log.debug("msg=[Updating members of group {}: making {} modification(s).]",
            groupName, mods.size());
        new ModifyOperation(conn).execute(
            new ModifyRequest(
                ldapEntry.getDn(),
                mods.toArray(new AttributeModification[0])));
      }

      return mapper.mapLdapEntryToSambaGroup(ldapEntry);

    } catch (final LdapException e) {
      final ServiceException se = internalServerError(
          "Updating samba group members failed.",
          e);
      log.error("msg=[Updating samba group members failed.] group=[{}]", groupName, se);
      throw se;

    } finally {
      closeConnection(conn);
    }
  }

  @Override
  public void deleteGroup(@NotNull final String groupName) {

    log.info("msg=[Deleting samba group.] group=[{}]", groupName);
    sambaTool.deleteGroup(groupName);
  }

  @Override
  public boolean userExists(@NotNull final String userName) {

    log.info("msg=[Checking whether samba user exists.] user=[{}]", userName);
    Connection conn = null;
    try {
      conn = getConnection();
      final boolean result = findUserByName(userName, conn).isPresent();
      log.info("msg=[Samba user [{}] exists? {}]", userName, result);
      return result;

    } catch (final LdapException e) {
      final ServiceException se = internalServerError(
          "Checking whether samba user exists failed.",
          e);
      log.error("msg=[Checking whether samba user exists failed.]", se);
      throw se;

    } finally {
      closeConnection(conn);
    }
  }

  @Override
  public SambaUser addUser(@Valid final SambaUserAddRequest sambaUser) {

    log.info("msg=[Adding samba user.] user=[{}]", sambaUser);
    try {
      getUser(sambaUser.getUserName());
      log.debug("msg=[Samba user {} already exists. Updating it ...]", sambaUser.getUserName());
      final Password password = new Password();
      password.setValue(sambaUser.getPassword());
      updateUserPassword(sambaUser.getUserName(), password);
      return updateUser(sambaUser.getUserName(), sambaUser);

    } catch (final NotFoundException nfe) {

      sambaTool.createUser(
          sambaUser.getUserName(),
          sambaUser.getPassword(),
          sambaUser.getDisplayName(),
          sambaUser.getEmail(),
          sambaUser.getMobile());
      final SambaUser user = updateUser(sambaUser.getUserName(), sambaUser);
      log.info("msg=[Samba user successfully added.] user=[{}]", user);
      return user;
    }
  }

  private Optional<LdapEntry> findUserByName(
      @NotNull final String userName,
      @NotNull final Connection conn) throws LdapException {

    final SearchFilter sf = new SearchFilter(properties.getUserFindOneFilter());
    sf.setParameters(new Object[]{userName});
    final SearchRequest sr = new SearchRequest(properties.getUserBaseDn(), sf);
    sr.setSearchScope(properties.getUserFindOneSearchScope());
    final SearchOperation so = new SearchOperation(conn);
    final SearchResult searchResult = so.execute(sr).getResult();
    final LdapEntry ldapEntry = searchResult.getEntry();
    return ldapEntry == null ? Optional.empty() : Optional.of(ldapEntry);
  }

  @Override
  public SambaUser getUser(@NotNull final String userName) {

    log.info("msg=[Getting samba user by name.] name=[{}]", userName);
    Connection conn = null;
    try {
      conn = getConnection();
      final SambaUser user = mapper.mapLdapEntryToSambaUser(
          findUserByName(userName, conn).orElseThrow(NotFoundException::new));
      log.info("msg=[Getting samba user by name.] name=[{}] user=[{}]", userName, user);
      return user;

    } catch (final LdapException e) {
      final ServiceException se = internalServerError(
          "Getting samba user by name failed.",
          e);
      log.error("msg=[Getting samba user by name failed.]", se);
      throw se;

    } finally {
      closeConnection(conn);
    }
  }

  @Override
  public SambaUser updateUser(@NotNull final String userName, @Valid final SambaUser sambaUser) {

    log.info("msg=[Updating samba user.] name=[{}] user=[{}]", userName, sambaUser);
    Connection conn = null;
    try {
      conn = getConnection();
      final LdapEntry ldapEntry = findUserByName(userName, conn)
          .orElseThrow(NotFoundException::new);

      int userAccountControl = getUserAccountControl(ldapEntry);
      if ((userAccountControl & UF_NORMAL_ACCOUNT) != UF_NORMAL_ACCOUNT) {
        userAccountControl = userAccountControl + UF_NORMAL_ACCOUNT;
      }
      if ((userAccountControl & UF_DONT_EXPIRE_PASSWD) != UF_DONT_EXPIRE_PASSWD) {
        userAccountControl = userAccountControl + UF_DONT_EXPIRE_PASSWD;
      }
      if (sambaUser.isEnabled() &&
          ((userAccountControl & UF_ACCOUNT_DISABLED) == UF_ACCOUNT_DISABLED)) {
        userAccountControl = userAccountControl - UF_ACCOUNT_DISABLED;
      } else if (!sambaUser.isEnabled() &&
          ((userAccountControl & UF_ACCOUNT_DISABLED) != UF_ACCOUNT_DISABLED)) {
        userAccountControl = userAccountControl + UF_ACCOUNT_DISABLED;
      }

      final List<AttributeModification> mods = new ArrayList<>();
      updateAttribute(ldapEntry, "displayName", sambaUser.getDisplayName(), mods);
      updateAttribute(ldapEntry, "gecos", sambaUser.getDisplayName(), mods);
      updateAttribute(ldapEntry, "mail", sambaUser.getEmail(), mods);
      updateAttribute(ldapEntry, "telephoneNumber", sambaUser.getMobile(), mods);
      updateAttribute(
          ldapEntry, "userAccountControl", String.valueOf(userAccountControl), mods);

      if (!mods.isEmpty()) {
        log.debug("msg=[Updating samba user [{}]: making {} modification(s).]",
            userName, mods.size());
        new ModifyOperation(conn).execute(
            new ModifyRequest(
                ldapEntry.getDn(),
                mods.toArray(new AttributeModification[0])));
      }

      updateUserGroups(ldapEntry, sambaUser.getGroups(), conn);

      final SambaUser user = mapper.mapLdapEntryToSambaUser(ldapEntry);
      log.info("msg=[Samba user successfully updated.] name=[{}] user=[{}]", userName, user);
      return user;

    } catch (final LdapException e) {
      final ServiceException se = internalServerError(
          "Getting user by name failed.",
          e);
      log.error("msg=[Getting user by name failed.]", se);
      throw se;

    } finally {
      closeConnection(conn);
    }
  }

  private void updateUserGroups(
      @NotNull final LdapEntry ldapEntry,
      @NotNull final List<Name> groups,
      @NotNull final Connection conn) throws LdapException {

    final String userName = getAttributeValue(
        ldapEntry, "sAMAccountName", "<unknown>");
    final LdapAttribute userGroupAttr = ldapEntry.getAttribute(properties.getUserGroupAttr());
    final boolean hasUserGroupAttr = userGroupAttr != null;
    final Set<String> oldGroupDns = hasUserGroupAttr
        ? new HashSet<>(userGroupAttr.getStringValues())
        : new HashSet<>();
    log.debug("msg=[Updating groups of user.] name=[{}] oldGroups=[{}]", userName, oldGroupDns);

    final Set<String> newGroupDns = groups
        .stream()
        .map(name -> name.isDistinguishedName()
            ? name.getValue()
            : createDn(properties.getGroupRdn(), name.getValue(), properties.getGroupBaseDn()))
        .collect(Collectors.toSet());
    log.debug("msg=[Updating groups of user.] name=[{}] newGroups=[{}]", userName, newGroupDns);

    final Set<String> both = new HashSet<>(oldGroupDns);
    both.retainAll(newGroupDns);
    log.debug("msg=[Updating groups of user.] name=[{}] keptGroups=[{}]", userName, both);

    oldGroupDns.removeAll(both);
    log.debug("msg=[Updating groups of user.] name=[{}] removeOldGroups=[{}]",
        userName, oldGroupDns);
    if (hasUserGroupAttr) {
      userGroupAttr.removeStringValues(oldGroupDns);
    }

    newGroupDns.removeAll(both);
    log.debug("msg=[Updating groups of user.] name=[{}] addNewGroups=[{}]", userName, newGroupDns);
    if (hasUserGroupAttr) {
      userGroupAttr.addStringValues(newGroupDns);
    } else if (!newGroupDns.isEmpty()) {
      ldapEntry.addAttribute(new LdapAttribute(
          properties.getUserGroupAttr(),
          newGroupDns.toArray(new String[0])));
    }

    // We only have to modify the groups, the attribute 'memberOf' in the user entry is just a link.
    for (final String groupDn : oldGroupDns) {
      new ModifyOperation(conn).execute(
          new ModifyRequest(
              groupDn,
              new AttributeModification(
                  AttributeModificationType.REMOVE,
                  new LdapAttribute(properties.getGroupMemberAttr(), ldapEntry.getDn()))));
    }

    // We only have to modify the groups, the attribute 'memberOf' in the user entry is just a link.
    for (final String groupDn : newGroupDns) {
      new ModifyOperation(conn).execute(
          new ModifyRequest(
              groupDn,
              new AttributeModification(
                  AttributeModificationType.ADD,
                  new LdapAttribute(properties.getGroupMemberAttr(), ldapEntry.getDn()))));
    }
  }

  @Override
  public SambaUser updateUserGroups(@NotNull final String userName, @Valid final Names groups) {

    log.info("msg=[Updating samba user's groups.] name=[{}] groups=[{}]", userName, groups);
    Connection conn = null;
    try {
      conn = getConnection();
      final LdapEntry ldapEntry = findUserByName(userName, conn)
          .orElseThrow(NotFoundException::new);
      updateUserGroups(ldapEntry, groups.getValues(), conn);
      final SambaUser user = mapper.mapLdapEntryToSambaUser(ldapEntry);
      log.info("Samba user's group successfully updated: {}", user);
      return user;

    } catch (final LdapException e) {
      final ServiceException se = internalServerError(
          "Updating samba user's group failed.",
          e);
      log.error("msg=[Updating samba user's group failed.]", se);
      throw se;

    } finally {
      closeConnection(conn);
    }
  }

  @Override
  public void updateUserPassword(
      @NotNull final String userName,
      @Valid final Password newPassword) {

    log.info("msg=[Updating samba user's password.] name=[{}]", userName);
    sambaTool.setNewPassword(userName, newPassword.getValue());
  }

  @Override
  public void deleteUser(@NotNull final String userName) {

    log.info("msg=[Deleting samba user.] name=[{}]", userName);
    sambaTool.deleteUser(userName);
  }

  private Connection getConnection() throws LdapException {
    final Connection c = this.connectionFactory.getConnection();
    if (!c.isOpen()) {
      c.open();
    }
    return c;
  }

  /**
   * Close the given context and ignore any thrown exception. This is useful for typical finally
   * blocks in manual Ldap statements.
   *
   * @param connection the Ldap connection to close
   */
  private void closeConnection(final Connection connection) {
    if (connection != null && connection.isOpen()) {
      try {
        connection.close();
      } catch (final Exception ex) {
        log.warn("Closing ldap connection failed.", ex);
      }
    }
  }

  @Override
  public List<DnsZone> getDnsZones() {
    log.info("msg=[Getting name server zones.]");
    return sambaTool.getDnsZones();
  }

  @Override
  public void createDnsZone(@NotNull final String zoneName) {
    log.info("msg=[Creating name server zone] zone=[{}]", zoneName);
    sambaTool.createDnsZone(zoneName);
  }

  @Override
  public void deleteDnsZone(@NotNull final String zoneName) {
    log.info("msg=[Deleting name server zone] zone=[{}]", zoneName);
    sambaTool.deleteDnsZone(zoneName);
  }

  @Override
  public List<DnsEntry> getDnsRecords(@NotNull final String zoneName) {
    log.info("msg=[Getting name server records.] zone=[{}]", zoneName);
    return sambaTool.getDnsRecords(zoneName);
  }

  @Override
  public void addDnsRecord(
      @NotNull final String zoneName,
      @NotNull final String name,
      @NotNull final DnsRecordType recordType,
      @NotNull final String data) {

    log.info("msg=[Adding name server record] zoneName=[{}] name=[{}] recordType=[{}] data=[{}]",
        zoneName, name, recordType, data);
    sambaTool.addDnsRecord(zoneName, name, recordType, data);
  }

  @Override
  public void deleteDnsRecord(
      @NotNull final String zoneName,
      @NotNull final String name,
      @NotNull final DnsRecordType recordType,
      @NotNull final String data) {

    log.info("msg=[Deleting name server record] zoneName=[{}] name=[{}] recordType=[{}] data=[{}]",
        zoneName, name, recordType, data);
    sambaTool.deleteDnsRecord(zoneName, name, recordType, data);
  }

  @Override
  public void updateDnsRecord(
      @NotNull final String zoneName,
      @NotNull final String name,
      @NotNull final DnsRecordType recordType,
      @NotNull final String oldData,
      @NotNull final String newData) {

    log.info("msg=[Deleting name server record] zoneName=[{}] name=[{}] recordType=[{}] "
            + "oldData=[{}], newData=[{}]",
        zoneName, name, recordType, newData, oldData);
    sambaTool.updateDnsRecord(zoneName, name, recordType, oldData, newData);
  }

}
