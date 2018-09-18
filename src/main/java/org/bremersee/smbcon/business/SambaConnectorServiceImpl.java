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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
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

  private final DnsZoneComparator dnsZoneComparator = new DnsZoneComparator();

  private final DnsEntryComparator dnsEntryComparator = new DnsEntryComparator();

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
    return sambaTool.getDnsZones()
        .stream()
        .filter(this::isNonExcludedDnsZone)
        .sorted(dnsZoneComparator)
        .collect(Collectors.toList());
  }

  @Override
  public List<DnsZone> getDnsReverseZones() {
    log.info("msg=[Getting name server reverse zones.]");
    return sambaTool.getDnsZones()
        .stream()
        .filter(this::isNonExcludedDnsZone)
        .filter(this::isDnsReverseZone)
        .sorted(dnsZoneComparator)
        .collect(Collectors.toList());
  }

  @Override
  public List<DnsZone> getDnsNonReverseZones() {
    log.info("msg=[Getting name server non reverse zones.]");
    return sambaTool.getDnsZones()
        .stream()
        .filter(this::isNonExcludedDnsZone)
        .filter(this::isNonDnsReverseZone)
        .sorted(dnsZoneComparator)
        .collect(Collectors.toList());
  }

  private Optional<DnsZone> findDnsReverseZone(@NotNull final String ip) {
    return getDnsReverseZones()
        .stream()
        .filter(dnsZone -> ipMatchesDnsReverseZone(ip, dnsZone))
        .findFirst();
  }

  private Optional<DnsZone> findDnsZone(@NotNull final String domainName) {
    final List<DnsZone> zones = getDnsNonReverseZones();
    String domain = domainName;
    int index = domain.indexOf('.');
    while (index > 0 && domain.length() > index + 1) {
      domain = domain.substring(index + 1);
      for (DnsZone zone : zones) {
        if (domain.equals(zone.getPszZoneName())) {
          return Optional.of(zone);
        }
      }
      index = domain.indexOf('.');
    }
    return Optional.empty();
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
    return sambaTool
        .getDnsRecords(zoneName)
        .stream()
        .filter(this::isNonExcludedDnsEntry)
        .sorted(dnsEntryComparator)
        .collect(Collectors.toList());
  }

  @Override
  public boolean dnsRecordExists(
      @NotNull final String zoneName,
      @NotNull final String name,
      @NotNull final DnsRecordType recordType,
      @NotNull final String data) {
    return sambaTool.getDnsRecords(zoneName)
        .parallelStream()
        .anyMatch(entry -> entry
            .getName().equals(name)
            && entry.getRecords()
            .parallelStream()
            .anyMatch(record -> recordType.name()
                .equals(record.getRecordType())
                && data.equals(record.getRecordValue())));
  }

  private void doAddDnsRecord(
      @NotNull final String zoneName,
      @NotNull final String name,
      @NotNull final DnsRecordType recordType,
      @NotNull final String data) {

    log.info("msg=[Adding name server record] zoneName=[{}] name=[{}] recordType=[{}] data=[{}]",
        zoneName, name, recordType, data);
    if (!dnsRecordExists(zoneName, name, recordType, data)) {
      sambaTool.addDnsRecord(zoneName, name, recordType, data);
    }
  }

  @Override
  public void addDnsRecord(
      @NotNull final String zoneName,
      @NotNull final String name,
      @NotNull final DnsRecordType recordType,
      @NotNull final String data) {

    if (isDnsReverseZone(zoneName)) {
      doAddDnsRecord(zoneName, name, recordType, data);
      if (DnsRecordType.PTR.equals(recordType)) {
        // zone name is something like 1.168.192.in-addr.arpa
        // name is the end of the ip address,
        // e. g.
        //       113   in  1.168.192.in-addr.arpa
        // or
        //       113.1 in  168.192.in-addr.arpa
        // data is the full domain name, e. g. forelle.eixe.bremersee.org
        findDnsZone(data)
            .ifPresent(dnsZone -> doAddDnsRecord(
                dnsZone.getPszZoneName(), // something like eixe.bremersee.org
                data.substring(0, data.length() - (dnsZone.getPszZoneName().length() + 1)),
                DnsRecordType.A,
                name + "." + zoneName
                    .substring(
                        0,
                        zoneName.length() - properties.getReverseZoneSuffix().length())));
      }
    } else {
      doAddDnsRecord(zoneName, name, recordType, data);
      if (DnsRecordType.A.equals(recordType)) {
        // zone name equals the domain, e. g. eixe.bremersee.org
        // name is the host name, e. g. forelle
        // data is the ip address, e. g. 192.168.1.113
        findDnsReverseZone(data)
            .ifPresent(dnsZone -> doAddDnsRecord(
                dnsZone.getPszZoneName(),
                getDnsReverseEntryName(data, dnsZone.getPszZoneName()),
                DnsRecordType.PTR,
                name + "." + zoneName));
      }
    }
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

  private boolean isNonExcludedDnsZone(DnsZone zone) {
    return zone != null && !isExcludedDnsZone(zone);
  }

  private boolean isExcludedDnsZone(DnsZone zone) {
    return zone != null && isExcludedDnsZone(zone.getPszZoneName());
  }

  private boolean isExcludedDnsZone(String zoneName) {
    if (zoneName == null) {
      return true;
    }
    for (final String regex : properties.getExcludedZoneRegexList()) {
      if (Pattern.compile(regex).matcher(zoneName).matches()) {
        return true;
      }
    }
    return false;
  }

  private boolean isNonExcludedDnsEntry(DnsEntry entry) {
    return entry != null && !isExcludedDnsEntry(entry);
  }

  private boolean isExcludedDnsEntry(DnsEntry entry) {
    return entry != null && isExcludedDnsEntry(entry.getName());
  }

  private boolean isExcludedDnsEntry(String entryName) {
    if (entryName == null) {
      return true;
    }
    for (final String regex : properties.getExcludedEntryRegexList()) {
      if (Pattern.compile(regex).matcher(entryName).matches()) {
        return true;
      }
    }
    return false;
  }

  private boolean isNonDnsReverseZone(DnsZone zone) {
    return zone != null && !isDnsReverseZone(zone.getPszZoneName());
  }

  private boolean isDnsReverseZone(DnsZone zone) {
    return zone != null && isDnsReverseZone(zone.getPszZoneName());
  }

  private boolean isDnsReverseZone(String zoneName) {
    return zoneName != null && zoneName.endsWith(properties.getReverseZoneSuffix());
  }

  private boolean ipMatchesDnsReverseZone(String ip, DnsZone zone) {
    return zone != null && ipMatchesDnsReverseZone(ip, zone.getPszZoneName());
  }

  private boolean ipMatchesDnsReverseZone(String ip, String zoneName) {
    if (ip != null && isDnsReverseZone(zoneName)) {
      final String tmp = zoneName
          .substring(0, zoneName.length() - properties.getReverseZoneSuffix().length());
      final String[] reverseParts = tmp.split(Pattern.quote("."));
      final StringBuilder ipBuilder = new StringBuilder();
      for (int i = reverseParts.length - 1; i >= 0; i--) {
        ipBuilder.append(reverseParts[i]).append('.');
      }
      return ip.startsWith(ipBuilder.toString());
    }
    return false;
  }

  private String getDnsReverseEntryName(String ip, String zoneName) {
    if (ip != null && isDnsReverseZone(zoneName)) {
      final String tmp = zoneName
          .substring(0, zoneName.length() - properties.getReverseZoneSuffix().length());
      final String[] reverseParts = tmp.split(Pattern.quote("."));
      final StringBuilder ipBuilder = new StringBuilder();
      for (int i = reverseParts.length - 1; i >= 0; i--) {
        ipBuilder.append(reverseParts[i]).append('.');
      }
      final String ipPrefix = ipBuilder.toString();
      if (ipPrefix.length() < ip.length()) {
        return ip.substring(ipPrefix.length());
      }
    }
    return null;
  }

  private class DnsZoneComparator implements Comparator<DnsZone> {

    @Override
    public int compare(DnsZone o1, DnsZone o2) {
      final String s1 = o1 != null && o1.getPszZoneName() != null ? o1.getPszZoneName() : "";
      final String s2 = o2 != null && o2.getPszZoneName() != null ? o2.getPszZoneName() : "";
      if (isDnsReverseZone(s1) && isDnsReverseZone(s2)) {
        final String[] sa1 = s1.split(Pattern.quote("."));
        final String[] sa2 = s2.split(Pattern.quote("."));
        int c = sa2.length - sa1.length;
        if (c != 0) {
          return c;
        }
        return compare(sa1, sa2);
      } else if (!isDnsReverseZone(s1) && isDnsReverseZone(s2)) {
        return -1;
      } else if (isDnsReverseZone(s1) && !isDnsReverseZone(s2)) {
        return 1;
      }
      return s1.compareTo(s2);
    }

    private int compare(String[] sa1, String[] sa2) {
      final String[] sav1 = sa1 != null ? sa1 : new String[0];
      final String[] sav2 = sa2 != null ? sa2 : new String[0];
      final int len = Math.min(sav1.length, sav2.length);
      for (int i = len - 1; i >= 0; i--) {
        int c = compare(sav1[i], sav2[i]);
        if (c != 0) {
          return c;
        }
      }
      return 0;
    }

    private int compare(String s1, String s2) {
      try {
        return compare(Integer.parseInt(s1), Integer.parseInt(s2));
      } catch (final Throwable t) {
        return (s1 != null ? s1 : "").compareToIgnoreCase(s2 != null ? s2 : "");
      }
    }

    private int compare(int i1, int i2) {
      return Integer.compare(i1, i2);
    }
  }

  private class DnsEntryComparator implements Comparator<DnsEntry> {

    @Override
    public int compare(DnsEntry o1, DnsEntry o2) {
      final String s1 = o1 != null && o1.getName() != null ? o1.getName() : "";
      final String s2 = o2 != null && o2.getName() != null ? o2.getName() : "";
      try {
        return Integer.compare(Integer.parseInt(s1), Integer.parseInt(s2));
      } catch (final Throwable t) {
        return s1.compareToIgnoreCase(s2);
      }
    }
  }

}
