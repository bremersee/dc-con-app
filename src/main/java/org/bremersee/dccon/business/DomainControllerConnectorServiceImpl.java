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
import static org.bremersee.dccon.business.LdapEntryUtils.UF_DONT_EXPIRE_PASSWORD;
import static org.bremersee.dccon.business.LdapEntryUtils.UF_NORMAL_ACCOUNT;
import static org.bremersee.dccon.business.LdapEntryUtils.createDn;
import static org.bremersee.dccon.business.LdapEntryUtils.getAttributeValue;
import static org.bremersee.dccon.business.LdapEntryUtils.getUserAccountControl;
import static org.bremersee.dccon.business.LdapEntryUtils.updateAttribute;
import static org.bremersee.exception.ServiceException.internalServerError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.comparator.ComparatorBuilder;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.exception.GroupAlreadyExistsException;
import org.bremersee.dccon.exception.GroupNotFoundException;
import org.bremersee.dccon.exception.NotFoundException;
import org.bremersee.dccon.exception.UserNotFoundException;
import org.bremersee.dccon.model.AddDhcpLeaseParameter;
import org.bremersee.dccon.model.CorrelatedDnsRecord;
import org.bremersee.dccon.model.DhcpLease;
import org.bremersee.dccon.model.DnsEntry;
import org.bremersee.dccon.model.DnsRecord;
import org.bremersee.dccon.model.DnsRecordType;
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.model.DomainGroupItem;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.model.Password;
import org.bremersee.exception.ServiceException;
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
import org.springframework.context.annotation.Profile;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * The domain connector service implementation.
 *
 * @author Christian Bremer
 */
@SuppressWarnings("Duplicates")
@Profile("!in-memory")
@Component
@Slf4j
public class DomainControllerConnectorServiceImpl implements DomainControllerConnectorService {

  private final DnsZoneComparator dnsZoneComparator = new DnsZoneComparator();

  private final DnsEntryComparator dnsEntryComparator = new DnsEntryComparator();

  private final DomainControllerProperties properties;

  private final LdapEntryMapper mapper;

  private final ConnectionFactory connectionFactory;

  private final SambaTool sambaTool;

  private final DhcpLeaseList dhcpLeaseList;

  /**
   * Instantiates a new Domain connector service.
   *
   * @param properties        the properties
   * @param mapper            the mapper
   * @param connectionFactory the connection factory
   * @param sambaTool         the samba tool
   * @param dhcpLeaseList     the dhcp lease list
   */
  public DomainControllerConnectorServiceImpl(
      final DomainControllerProperties properties,
      final LdapEntryMapper mapper,
      final ConnectionFactory connectionFactory,
      final SambaTool sambaTool,
      final DhcpLeaseList dhcpLeaseList) {

    this.properties = properties;
    this.mapper = mapper;
    this.connectionFactory = connectionFactory;
    this.sambaTool = sambaTool;
    this.dhcpLeaseList = dhcpLeaseList;
  }

  @Override
  public List<DomainGroupItem> getGroups() {

    log.info("msg=[Getting domain groups]");
    final SearchFilter sf = new SearchFilter(properties.getGroupFindAllFilter());
    final SearchRequest sr = new SearchRequest(properties.getGroupBaseDn(), sf);
    sr.setSearchScope(properties.getGroupFindAllSearchScope());
    Connection conn = null;
    try {
      conn = getConnection();
      final SearchOperation so = new SearchOperation(conn);
      final SearchResult searchResult = so.execute(sr).getResult();
      final List<DomainGroupItem> groups = searchResult.getEntries()
          .stream()
          .map(mapper::mapLdapEntryToDomainGroupItem)
          .collect(Collectors.toList());
      log.info("msg=[Getting domain groups] resultSize=[{}]", groups.size());
      return groups;

    } catch (final LdapException e) {
      final ServiceException se = internalServerError(
          "Getting domain groups failed.",
          e);
      log.error("msg=[Getting domain groups failed.]", se);
      throw se;

    } finally {
      closeConnection(conn);
    }
  }

  @Override
  public DomainGroup addGroup(
      @Valid final DomainGroup group) {

    log.info("msg=[Adding domain group] group=[{}]", group);
    try {
      getGroupByName(group.getName());
      throw new GroupAlreadyExistsException(group.getName());

    } catch (final NotFoundException nfe) {

      sambaTool.addGroup(group.getName());
      return updateGroupMembers(group.getName(), group.getMembers());
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
  public DomainGroup getGroupByName(
      @NotNull final String groupName) {

    log.info("msg=[Getting domain group by name] name=[{}]", groupName);
    Connection conn = null;
    try {
      conn = getConnection();
      final DomainGroup group = mapper
          .mapLdapEntryToDomainGroup(findGroupByName(groupName, conn)
              .orElseThrow(GroupNotFoundException.supplier(groupName)));
      log.info("msg=[Getting domain group by name] name=[{}] group=[{}]", groupName, group);
      return group;

    } catch (final LdapException e) {
      final ServiceException se = internalServerError(
          "Getting domain group by name failed.",
          e);
      log.error("msg=[Getting domain group by name failed.]", se);
      throw se;

    } finally {
      closeConnection(conn);
    }
  }

  @Override
  public DomainGroup updateGroupMembers(
      @NotNull final String groupName,
      @Valid final List<String> members) {

    log.info("msg=[Updating domain group members] group=[{}] members=[{}]", groupName, members);
    Connection conn = null;
    try {
      conn = getConnection();
      LdapEntry ldapEntry = findGroupByName(groupName, conn)
          .orElseThrow(GroupNotFoundException.supplier(groupName));

      final LdapAttribute memberAttr = ldapEntry.getAttribute(properties.getGroupMemberAttr());
      final boolean hasMemberAttr = memberAttr != null;
      final Set<String> oldUserDns = hasMemberAttr
          ? new HashSet<>(memberAttr.getStringValues())
          : new HashSet<>();
      log.debug("msg=[Updating members of group] group=[{}] oldMembers=[{}]",
          groupName, oldUserDns);

      final Set<String> newUserDns = members == null || members.isEmpty()
          ? new HashSet<>()
          : members
              .stream()
              .map(member -> createDn(properties.getUserRdn(), member, properties.getUserBaseDn()))
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

      return mapper.mapLdapEntryToDomainGroup(ldapEntry);

    } catch (final LdapException e) {
      final ServiceException se = internalServerError(
          "Updating domain group members failed.",
          e);
      log.error("msg=[Updating domain group members failed.] group=[{}]", groupName, se);
      throw se;

    } finally {
      closeConnection(conn);
    }
  }

  @Override
  public void deleteGroup(@NotNull final String groupName) {

    log.info("msg=[Deleting domain group.] group=[{}]", groupName);
    sambaTool.deleteGroup(groupName);
  }

  @Override
  public boolean userExists(@NotNull final String userName) {

    log.info("msg=[Checking whether domain user exists.] user=[{}]", userName);
    Connection conn = null;
    try {
      conn = getConnection();
      final boolean result = findUserByName(userName, conn).isPresent();
      log.info("msg=[Domain user [{}] exists? {}]", userName, result);
      return result;

    } catch (final LdapException e) {
      final ServiceException se = internalServerError(
          "Checking whether domain user exists failed.",
          e);
      log.error("msg=[Checking whether domain user exists failed.]", se);
      throw se;

    } finally {
      closeConnection(conn);
    }
  }

  @Override
  public DomainUser addUser(
      @Valid final DomainUser domainUser) {

    log.info("msg=[Adding domain user.] user=[{}]", domainUser);
    try {
      getUser(domainUser.getUserName());
      log.debug("msg=[Domain user {} already exists. Updating it ...]", domainUser.getUserName());
      final Password password = new Password();
      password.setValue(domainUser.getPassword());
      updateUserPassword(domainUser.getUserName(), password);
      return updateUser(domainUser.getUserName(), domainUser, true);

    } catch (final NotFoundException nfe) {

      sambaTool.createUser(
          domainUser.getUserName(),
          domainUser.getPassword(),
          domainUser.getFirstName(),
          domainUser.getLastName(),
          domainUser.getDisplayName(),
          domainUser.getEmail(),
          domainUser.getTelephoneNumber());
      final DomainUser user = updateUser(domainUser.getUserName(), domainUser, true);
      log.info("msg=[Domain user successfully added.] user=[{}]", user);
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
  public DomainUser getUser(@NotNull final String userName) {

    log.info("msg=[Getting domain user by name.] name=[{}]", userName);
    Connection conn = null;
    try {
      conn = getConnection();
      final DomainUser user = mapper
          .mapLdapEntryToDomainUser(findUserByName(userName, conn)
              .orElseThrow(UserNotFoundException.supplier(userName)));
      log.info("msg=[Getting domain user by name.] name=[{}] user=[{}]", userName, user);
      return user;

    } catch (final LdapException e) {
      final ServiceException se = internalServerError(
          "Getting domain user by name failed.",
          e);
      log.error("msg=[Getting domain user by name failed.]", se);
      throw se;

    } finally {
      closeConnection(conn);
    }
  }

  @Override
  public DomainUser updateUser(
      @NotNull final String userName,
      @Valid final DomainUser domainUser,
      @Nullable final Boolean updateGroups) {

    log.info("msg=[Updating domain user.] name=[{}] user=[{}]", userName, domainUser);
    Connection conn = null;
    try {
      conn = getConnection();
      final LdapEntry ldapEntry = findUserByName(userName, conn)
          .orElseThrow(UserNotFoundException.supplier(userName));

      int userAccountControl = getUserAccountControl(ldapEntry);
      if ((userAccountControl & UF_NORMAL_ACCOUNT) != UF_NORMAL_ACCOUNT) {
        userAccountControl = userAccountControl + UF_NORMAL_ACCOUNT;
      }
      if ((userAccountControl & UF_DONT_EXPIRE_PASSWORD) != UF_DONT_EXPIRE_PASSWORD) {
        userAccountControl = userAccountControl + UF_DONT_EXPIRE_PASSWORD;
      }
      if (domainUser.getEnabled() &&
          ((userAccountControl & UF_ACCOUNT_DISABLED) == UF_ACCOUNT_DISABLED)) {
        userAccountControl = userAccountControl - UF_ACCOUNT_DISABLED;
      } else if (!domainUser.getEnabled() &&
          ((userAccountControl & UF_ACCOUNT_DISABLED) != UF_ACCOUNT_DISABLED)) {
        userAccountControl = userAccountControl + UF_ACCOUNT_DISABLED;
      }

      final StringBuilder gecosBuilder = new StringBuilder();
      if (StringUtils.hasText(domainUser.getFirstName())) {
        gecosBuilder.append(domainUser.getFirstName());
        if (StringUtils.hasText(domainUser.getLastName())) {
          gecosBuilder.append(' ');
        }
      }
      if (StringUtils.hasText(domainUser.getLastName())) {
        gecosBuilder.append(domainUser.getLastName());
      }
      final String gecos = StringUtils.hasText(domainUser.getDisplayName())
          ? domainUser.getDisplayName() : gecosBuilder.toString();
      final List<AttributeModification> mods = new ArrayList<>();
      updateAttribute(ldapEntry, "displayName", gecos, mods);
      updateAttribute(ldapEntry, "sn", domainUser.getLastName(), mods);
      updateAttribute(ldapEntry, "givenName", domainUser.getFirstName(), mods);
      updateAttribute(ldapEntry, "gecos", gecos, mods);
      updateAttribute(ldapEntry, "mail", domainUser.getEmail(), mods);
      updateAttribute(ldapEntry, "telephoneNumber", domainUser.getTelephoneNumber(),
          mods);
      updateAttribute(ldapEntry, "mobile", domainUser.getMobile(), mods);
      updateAttribute(
          ldapEntry, "userAccountControl", String.valueOf(userAccountControl), mods);

      if (!mods.isEmpty()) {
        log.debug("msg=[Updating domain user [{}]: making {} modification(s).]",
            userName, mods.size());
        new ModifyOperation(conn).execute(
            new ModifyRequest(
                ldapEntry.getDn(),
                mods.toArray(new AttributeModification[0])));
      }

      if (Boolean.TRUE.equals(updateGroups)) {
        updateUserGroups(ldapEntry, domainUser.getGroups(), conn);
      }

      final DomainUser user = mapper.mapLdapEntryToDomainUser(ldapEntry);
      log.info("msg=[Domain user successfully updated.] name=[{}] user=[{}]", userName, user);
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
      @NotNull final List<String> groups,
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
        .map(name -> createDn(properties.getGroupRdn(), name, properties.getGroupBaseDn()))
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
  public DomainUser updateUserGroups(
      @NotNull final String userName,
      @Valid final List<String> groups) {

    log.info("msg=[Updating domain user's groups.] name=[{}] groups=[{}]", userName, groups);
    Connection conn = null;
    try {
      conn = getConnection();
      final LdapEntry ldapEntry = findUserByName(userName, conn)
          .orElseThrow(UserNotFoundException.supplier(userName));
      updateUserGroups(ldapEntry, groups, conn);
      final DomainUser user = mapper.mapLdapEntryToDomainUser(ldapEntry);
      log.info("Domain user's group successfully updated: {}", user);
      return user;

    } catch (final LdapException e) {
      final ServiceException se = internalServerError(
          "Updating domain user's group failed.",
          e);
      log.error("msg=[Updating domain user's group failed.]", se);
      throw se;

    } finally {
      closeConnection(conn);
    }
  }

  @Override
  public void updateUserPassword(
      @NotNull final String userName,
      @Valid final Password newPassword) {

    log.info("msg=[Updating domain user's password.] name=[{}]", userName);
    sambaTool.setNewPassword(userName, newPassword.getValue());
  }

  @Override
  public void deleteUser(@NotNull final String userName) {

    log.info("msg=[Deleting domain user.] name=[{}]", userName);
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
  public List<DhcpLease> getDhcpLeases(final Boolean all, final String sort) {

    final String sortOrder = StringUtils.hasText(sort) ? sort : DhcpLease.SORT_ORDER_BEGIN_HOSTNAME;
    final List<DhcpLease> leases = dhcpLeaseList.getDhcpLeases(all);
    leases.sort(ComparatorBuilder.builder()
        .fromWellKnownText(sortOrder)
        .build());
    return leases;
  }

  private Map<String, List<DhcpLease>> getDhcpLeasesMap(
      final AddDhcpLeaseParameter leases) {
    final Map<String, List<DhcpLease>> leasesMap;
    if (AddDhcpLeaseParameter.NONE == leases) {
      leasesMap = Collections.emptyMap();
    } else {
      leasesMap = new HashMap<>();
      final List<DhcpLease> leasesList = getDhcpLeases(
          AddDhcpLeaseParameter.ALL == leases,
          "ip|begin,desc");
      for (DhcpLease lease : leasesList) {
        leasesMap.computeIfAbsent(lease.getIp(), key -> new ArrayList<>()).add(lease);
      }
    }
    return leasesMap;
  }

  private DnsEntry addDhcpLeases(
      final Map<String, List<DhcpLease>> leasesMap,
      final DnsEntry dnsEntry,
      final String zoneName) {
    final List<DnsRecord> dnsRecords = dnsEntry.getRecords();
    if (dnsRecords == null) {
      return dnsEntry;
    }
    final HashSet<String> addedIps = new HashSet<>();
    for (DnsRecord dnsRecord : dnsRecords) {
      final String ip;
      final String hostname;
      if (isDnsReverseZone(zoneName)) {
        ip = getIpV4(dnsEntry.getName(), zoneName);
        int index = dnsRecord.getRecordValue().indexOf('.');
        if (index < 0) {
          index = dnsRecord.getRecordValue().length();
        }
        hostname = dnsRecord.getRecordValue().substring(0, index);
      } else {
        ip = dnsRecord.getRecordValue();
        hostname = dnsEntry.getName();
      }
      final List<DhcpLease> leases = leasesMap.get(ip);
      if (leases != null
          && !leases.isEmpty()
          && !addedIps.contains(ip)
          && leases.get(0).getHostname() != null
          && leases.get(0).getHostname().equals(hostname)) {
        dnsRecord.setDhcpLease(leases.get(0));
        addedIps.add(ip);
      }
    }
    return dnsEntry;
  }

  private Map<String, List<CorrelatedDnsRecord>> getDnsRecordsOfCorrelatedZones(
      final String zoneName, Boolean correlations) {
    if (Boolean.FALSE.equals(correlations)) {
      return Collections.emptyMap();
    }
    final Map<String, List<CorrelatedDnsRecord>> map = new HashMap<>();
    final boolean isReverseZone = isDnsReverseZone(zoneName);
    final String correlatedRecordType = isReverseZone
        ? DnsRecordType.A.name()
        : DnsRecordType.PTR.name();
    final Set<String> correlatedZoneNames = properties.findCorrelatedDnsZones(zoneName);
    for (final String correlatedZoneName : correlatedZoneNames) {
      final List<DnsEntry> entries = getDnsRecords(
          correlatedZoneName,
          false,
          AddDhcpLeaseParameter.NONE);
      for (final DnsEntry entry : entries) {
        for (final DnsRecord record : entry.getRecords()) {
          if (correlatedRecordType.equalsIgnoreCase(record.getRecordType())) {
            final CorrelatedDnsRecord correlatedRecord = CorrelatedDnsRecord.builder()
                .entryName(entry.getName())
                .flags(record.getFlags())
                .recordType(record.getRecordType())
                .recordValue(record.getRecordValue())
                .serial(record.getSerial())
                .ttl(record.getTtl())
                .zoneName(correlatedZoneName)
                .build();
            final String key;
            if (isDnsReverseZone(correlatedZoneName)) {
              key = getIpV4(correlatedRecord.getEntryName(), correlatedZoneName);
            } else {
              key = getFullQualifiedDomainName(correlatedRecord.getEntryName(), correlatedZoneName);
            }
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(correlatedRecord);
          }
        }
      }
    }
    return map;
  }

  private DnsEntry addCorrelatedDnsRecords(
      final String zoneName,
      final DnsEntry dnsEntry,
      final Map<String, List<CorrelatedDnsRecord>> correlatedDnsRecords) {

    for (final DnsRecord record : dnsEntry.getRecords()) {
      final Optional<CorrelatedDnsRecord> correlatedRecord = correlatedDnsRecords
          .getOrDefault(record.getRecordValue(), Collections.emptyList())
          .stream()
          .filter(cr -> isCorrelatedDnsRecord(
              zoneName, dnsEntry.getName(), record, cr))
          .findAny();
      if (correlatedRecord.isPresent()) {
        record.setCorrelatedDnsRecord(correlatedRecord.get());
        break;
      }
    }
    return dnsEntry;
  }

  private boolean isCorrelatedDnsRecord(
      final String zoneName,
      final String entryName,
      final DnsRecord record,
      final CorrelatedDnsRecord correlatedRecord) {

    if (log.isDebugEnabled()) {
      log.debug("msg=[Is correlated dns record?] zoneName=[{}] entryName=[{}] record=[{}]"
          + " correlatedRecord=[{}]", zoneName, entryName, record, correlatedRecord);
    }
    final boolean result;
    if (DnsRecordType.A.toString().equalsIgnoreCase(record.getRecordType())) {
      final String fqhn = getFullQualifiedDomainName(entryName, zoneName);
      log.debug("msg=[Equals? {} == {}]", fqhn, correlatedRecord.getRecordValue());
      result = fqhn.equalsIgnoreCase(correlatedRecord.getRecordValue());
    } else if (DnsRecordType.PTR.toString().equalsIgnoreCase(record.getRecordType())) {
      final String ip = getIpV4(entryName, zoneName);
      log.debug("msg=[Equals? {} == {}]", ip, correlatedRecord.getRecordValue());
      result = ip.equalsIgnoreCase(correlatedRecord.getRecordValue());
    } else {
      result = false;
    }
    log.debug("msg=[Is correlated dns record?] result=[{}]", result);
    return result;
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

  /**
   * Returns the dns reverse zone of an IPv4.
   *
   * @param ip the IPv4
   * @return the dns reverse zone or {@code empty}
   */
  private Optional<DnsZone> findDnsReverseZone(@NotNull final String ip) {
    return getDnsReverseZones()
        .stream()
        .filter(dnsZone -> ipMatchesDnsReverseZone(ip, dnsZone))
        .findFirst();
  }

  /**
   * Returns the zone of the domain name. The domain name can be a full qualified host name, e. g.
   * {@code forelle.eixe.bremersee.org}.
   *
   * @param domainName the domain name or a full qualified host name
   * @return the dns zone or {@code empty}
   */
  private Optional<DnsZone> findDnsZone(@NotNull final String domainName) {
    final List<DnsZone> zones = getDnsNonReverseZones();

    int index;
    String domain = domainName;
    do {
      for (DnsZone zone : zones) {
        if (domain.equals(zone.getPszZoneName())) {
          return Optional.of(zone);
        }
      }
      index = domain.indexOf('.');
      domain = domain.substring(index + 1);
    } while (index >= 0 && domain.length() > index + 1);
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
  public List<DnsEntry> getDnsRecords(
      final String zoneName,
      final Boolean correlations,
      final AddDhcpLeaseParameter leases) {
    log.info("msg=[Getting name server records.] zone=[{}] correlations=[{}] leases=[{}]",
        zoneName, correlations, leases);

    final Map<String, List<DhcpLease>> leasesMap = getDhcpLeasesMap(leases);
    final Map<String, List<CorrelatedDnsRecord>> correlatedDnsRecords
        = getDnsRecordsOfCorrelatedZones(zoneName, correlations);

    return sambaTool
        .getDnsRecords(zoneName)
        .stream()
        .filter(this::isNonExcludedDnsEntry)
        .map(dnsEntry -> addCorrelatedDnsRecords(zoneName, dnsEntry, correlatedDnsRecords))
        .map(dnsEntry -> addDhcpLeases(leasesMap, dnsEntry, zoneName))
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
      @NotNull final String data,
      @Nullable final Boolean alsoAddReverseRecord) {

    log.info("msg=[Adding name server record] "
            + "zoneName=[{}] name=[{}] recordType=[{}] data=[{}] alsoReverse=[{}]",
        zoneName, name, recordType, data, alsoAddReverseRecord);
    doAddDnsRecord(zoneName, name, recordType, data);
    if (Boolean.FALSE.equals(alsoAddReverseRecord)) {
      return;
    }
    final boolean isReverseZone = isDnsReverseZone(zoneName);
    if (isReverseZone && DnsRecordType.PTR.equals(recordType)) {
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
              getDnsEntryName(data, dnsZone.getPszZoneName()),
              DnsRecordType.A,
              getIpV4(name, zoneName)));
    } else if (!isReverseZone && DnsRecordType.A.equals(recordType)) {
      // zone name equals the domain, e. g. eixe.bremersee.org
      // name is the host name, e. g. forelle
      // data is the ip address, e. g. 192.168.1.113
      findDnsReverseZone(data)
          .ifPresent(dnsZone -> doAddDnsRecord(
              dnsZone.getPszZoneName(),
              getDnsReverseEntryName(data, dnsZone.getPszZoneName()),
              DnsRecordType.PTR,
              getFullQualifiedDomainName(name, zoneName)));
    }
  }

  @Override
  public void deleteDnsRecord(
      @NotNull final String zoneName,
      @NotNull final String name,
      @NotNull final DnsRecordType recordType,
      @NotNull final String data,
      @Nullable final Boolean alsoDeleteReverseRecord) {

    log.info("msg=[Deleting name server record] "
            + "zoneName=[{}] name=[{}] recordType=[{}] data=[{}] alsoReverse=[{}]",
        zoneName, name, recordType, data, alsoDeleteReverseRecord);
    sambaTool.deleteDnsRecord(zoneName, name, recordType, data);
    final boolean isReverseZone = isDnsReverseZone(zoneName);
    if (isReverseZone && DnsRecordType.PTR.equals(recordType)) {
      // zone name is something like 1.168.192.in-addr.arpa
      // name is the end of the ip address,
      // e. g.
      //       113   in  1.168.192.in-addr.arpa
      // or
      //       113.1 in  168.192.in-addr.arpa
      // data is the full domain name, e. g. forelle.eixe.bremersee.org
      findDnsZone(data)
          .ifPresent(dnsZone -> sambaTool.deleteDnsRecord(
              dnsZone.getPszZoneName(), // something like eixe.bremersee.org
              getDnsEntryName(data, dnsZone.getPszZoneName()),
              DnsRecordType.A,
              getIpV4(name, zoneName)));
    } else if (!isReverseZone && DnsRecordType.A.equals(recordType)) {
      // zone name equals the domain, e. g. eixe.bremersee.org
      // name is the host name, e. g. forelle
      // data is the ip address, e. g. 192.168.1.113
      findDnsReverseZone(data)
          .ifPresent(dnsZone -> sambaTool.deleteDnsRecord(
              dnsZone.getPszZoneName(),
              getDnsReverseEntryName(data, dnsZone.getPszZoneName()),
              DnsRecordType.PTR,
              getFullQualifiedDomainName(name, zoneName)));
    }
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

  private boolean isNonExcludedDnsZone(final DnsZone zone) {
    return zone != null && !isExcludedDnsZone(zone);
  }

  private boolean isExcludedDnsZone(final DnsZone zone) {
    return zone != null && isExcludedDnsZone(zone.getPszZoneName());
  }

  private boolean isExcludedDnsZone(final String zoneName) {
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

  private boolean isNonExcludedDnsEntry(final DnsEntry entry) {
    return entry != null && !isExcludedDnsEntry(entry);
  }

  private boolean isExcludedDnsEntry(final DnsEntry entry) {
    return entry != null && isExcludedDnsEntry(entry.getName());
  }

  private boolean isExcludedDnsEntry(final String entryName) {
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

  private boolean isNonDnsReverseZone(final DnsZone zone) {
    return zone != null && !isDnsReverseZone(zone.getPszZoneName());
  }

  private boolean isDnsReverseZone(final DnsZone zone) {
    return zone != null && isDnsReverseZone(zone.getPszZoneName());
  }

  private boolean isDnsReverseZone(final String zoneName) {
    final boolean result = zoneName != null && zoneName.endsWith(properties.getReverseZoneSuffix());
    log.debug("msg=[Is dns reverse zone?] zone=[{}] result=[{}]", zoneName, result);
    return result;
  }

  /**
   * Checks whether the given IPv4 (e. g. {@code 192.168.1.123}) matches the given dns reverse zone
   * (e. g. with name {@code 1.168.192.in-addr.arpa}).
   *
   * @param ip             the IPv4 (e. g. {@code 192.168.1.123})
   * @param dnsReverseZone the dns reverse zone (e. g. with name {@code 1.168.192.in-addr.arpa})
   * @return {@code true} if the ip matches the dns reverse zone, otherwise {@code false}
   */
  private boolean ipMatchesDnsReverseZone(final String ip, final DnsZone dnsReverseZone) {
    return dnsReverseZone != null && ipMatchesDnsReverseZone(ip, dnsReverseZone.getPszZoneName());
  }

  /**
   * Checks whether the given IPv4 (e. g. {@code 192.168.1.123}) matches the given dns reverse zone
   * name (e. g. {@code 1.168.192.in-addr.arpa}).
   *
   * @param ip                 the IPv4 (e. g. {@code 192.168.1.123})
   * @param dnsReverseZoneName the dns reverse zone name (e. g. {@code 1.168.192.in-addr.arpa})
   * @return {@code true} if the ip matches the dns reverse zone, otherwise {@code false}
   */
  private boolean ipMatchesDnsReverseZone(final String ip, final String dnsReverseZoneName) {
    if (ip != null && isDnsReverseZone(dnsReverseZoneName)) {
      final String ipPart = dnsReverseZoneName.substring(
          0,
          dnsReverseZoneName.length() - properties.getReverseZoneSuffix().length());
      final String[] ipParts = ipPart.split(Pattern.quote("."));
      final StringBuilder ipBuilder = new StringBuilder();
      for (int i = ipParts.length - 1; i >= 0; i--) {
        ipBuilder.append(ipParts[i]).append('.');
      }
      return ip.startsWith(ipBuilder.toString());
    }
    return false;
  }

  /**
   * Returns the host name without domain (= the name of a dns record in a dns zone). The given host
   * name must end with the given dns zone name.
   *
   * @param fullQualifiedHostName the full qualified host name, e. g. {@code forelle.eixe.bremersee.org}
   * @param dnsZoneName           the name of the dns zone, e. g. {@code eixe.bremersee.org}
   * @return the host name without domain
   */
  private String getDnsEntryName(final String fullQualifiedHostName, final String dnsZoneName) {
    Assert.hasText(fullQualifiedHostName,
        "Host name [" + fullQualifiedHostName + "] must not be null or empty.");
    Assert.hasText(dnsZoneName,
        "Dns zone name [" + dnsZoneName + "] must not be null or empty.");
    Assert.isTrue(fullQualifiedHostName.endsWith(dnsZoneName),
        "Host name [" + fullQualifiedHostName + "] must end with zone name ["
            + dnsZoneName + "].");
    return fullQualifiedHostName.substring(
        0,
        fullQualifiedHostName.length() - (dnsZoneName.length() + 1));
  }

  private String getFullQualifiedDomainName(final String hostName, final String dnsZoneName) {
    return hostName + "." + dnsZoneName;
  }

  /**
   * Returns the IPv4 from the given dns reverse record name (e. g. {@code 123}) and the given dns
   * reverse zone name (e. g. {@code 1.168.192.in-addr.arpa}).
   *
   * @param dnsReverseEntryName the dns reverse record name (e. g. {@code 123})
   * @param dnsReverseZoneName  the dns reverse zone name (e. g. {@code 1.168.192.in-addr.arpa})
   * @return the IPv4
   */
  private String getIpV4(final String dnsReverseEntryName, final String dnsReverseZoneName) {
    Assert.hasText(dnsReverseEntryName,
        "Dns reverse entry name [" + dnsReverseEntryName + "] must not be null or empty.");
    Assert.hasText(dnsReverseZoneName,
        "Dns reverse zone name [" + dnsReverseZoneName + "] must not be null or empty.");
    final String ipPart = dnsReverseZoneName.substring(
        0,
        dnsReverseZoneName.length() - properties.getReverseZoneSuffix().length());
    final String[] ipParts = ipPart.split(Pattern.quote("."));
    final StringBuilder ipBuilder = new StringBuilder();
    for (int i = ipParts.length - 1; i >= 0; i--) {
      ipBuilder.append(ipParts[i]).append('.');
    }
    ipBuilder.append(dnsReverseEntryName);
    final String ip = ipBuilder.toString();
    Assert.isTrue(ipMatchesDnsReverseZone(ip, dnsReverseZoneName),
        "IP [" + ip + "] must match dns reverse zone name [" + dnsReverseZoneName + "].");
    return ip;
  }

  /**
   * Returns the dns reverse entry name.
   *
   * @param ip                 the IPv4 (e. g. {@code 192.168.1.123}
   * @param dnsReverseZoneName the dns reverse zone name (e. g. {@code 1.168.192.in-addr.arpa}
   * @return the dns reverse entry name (e. g. {@code 123}
   */
  private String getDnsReverseEntryName(final String ip, final String dnsReverseZoneName) {
    Assert.hasText(ip, "IP must not be null or empty.");
    Assert.hasText(dnsReverseZoneName, "Dns reverse zone name must not be null or empty.");
    final String ipPart = dnsReverseZoneName.substring(
        0,
        dnsReverseZoneName.length() - properties.getReverseZoneSuffix().length());
    final String[] ipParts = ipPart.split(Pattern.quote("."));
    final StringBuilder ipBuilder = new StringBuilder();
    for (int i = ipParts.length - 1; i >= 0; i--) {
      ipBuilder.append(ipParts[i]).append('.');
    }
    final String ipPrefix = ipBuilder.toString();
    Assert.isTrue(ip.startsWith(ipPrefix),
        "IP [" + ip + "] must start with [" + ipPrefix + "].");
    return ip.substring(ipPrefix.length());
  }

  private static boolean isInteger(String value) {
    try {
      Integer.parseInt(value);
      return true;
    } catch (Exception ignored) {
      return false;
    }
  }

  private static String format(String value, @SuppressWarnings("SameParameterValue") int length) {
    final String format = "%0" + length + "d";
    if (value == null) {
      return "";
    } else if (isInteger(value)) {
      return String.format(format, Integer.parseInt(value));
    }
    return value;
  }

  private class DnsZoneComparator implements Comparator<DnsZone> {

    private final boolean asc;

    /**
     * Instantiates a new Dns zone comparator.
     */
    DnsZoneComparator() {
      this(Boolean.TRUE);
    }

    /**
     * Instantiates a new Dns zone comparator.
     *
     * @param asc the asc
     */
    DnsZoneComparator(Boolean asc) {
      this.asc = !Boolean.FALSE.equals(asc);
    }

    @Override
    public int compare(DnsZone o1, DnsZone o2) {
      final String s1 = o1 != null && o1.getPszZoneName() != null ? o1.getPszZoneName() : "";
      final String s2 = o2 != null && o2.getPszZoneName() != null ? o2.getPszZoneName() : "";
      log.debug("msg=[Comparing zones.] zone1=[{}] zone2=[{}]", s1, s2);
      if (isDnsReverseZone(s1) && isDnsReverseZone(s2)) {
        final String[] sa1 = s1.split(Pattern.quote("."));
        final String[] sa2 = s2.split(Pattern.quote("."));
        int c = sa2.length - sa1.length;
        if (c == 0) {
          c = compare(sa1, sa2);
        }
        log.debug("msg=[Both zones are dns reverse zones.] result=[{}]", c);
        return asc ? c : -1 * c;
      } else if (!isDnsReverseZone(s1) && isDnsReverseZone(s2)) {
        log.debug("msg=[First is non reverse zone, second is reverse zone.] "
            + "first=[{}] second=[{}] result=[-1]", s1, s2);
        return asc ? -1 : 1;
      } else if (isDnsReverseZone(s1) && !isDnsReverseZone(s2)) {
        log.debug("msg=[First is reverse zone, second is non reverse zone.] "
            + "first=[{}] second=[{}] result=[1]", s1, s2);
        return asc ? 1 : -1;
      }
      final int result = s1.compareToIgnoreCase(s2);
      log.debug("msg=[Both zones are non dns reverse zones.] result=[{}]", result);
      return asc ? result : -1 * result;
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
      return format(s1, 3).compareToIgnoreCase(format(s2, 3));
    }
  }

  private static class DnsEntryComparator implements Comparator<DnsEntry> {

    private final boolean asc;

    /**
     * Instantiates a dns entry comparator.
     */
    DnsEntryComparator() {
      this(Boolean.TRUE);
    }

    /**
     * Instantiates a dns entry comparator.
     *
     * @param asc use ascending sort order or not
     */
    DnsEntryComparator(Boolean asc) {
      this.asc = !Boolean.FALSE.equals(asc);
    }

    @Override
    public int compare(DnsEntry o1, DnsEntry o2) {
      final String s1 = o1 != null && o1.getName() != null ? o1.getName() : "";
      final String s2 = o2 != null && o2.getName() != null ? o2.getName() : "";

      final String[] sa1 = s1.split(Pattern.quote("."));
      final String[] sa2 = s2.split(Pattern.quote("."));
      if (sa1.length == sa2.length) {
        for (int i = 0; i < sa1.length; i++) {
          int result = format(sa1[i], 3).compareTo(format(sa2[i], 3));
          if (result != 0) {
            return asc ? result : -1 * result;
          }
        }
        return 0;
      }

      int result = format(s1, 3).compareToIgnoreCase(format(s2, 3));
      return asc ? result : -1 * result;
    }
  }

}
