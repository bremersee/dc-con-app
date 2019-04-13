/*
 * Copyright 2018 the original author or authors.
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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.exception.GroupAlreadyExistsException;
import org.bremersee.dccon.exception.GroupNotFoundException;
import org.bremersee.dccon.exception.NotFoundException;
import org.bremersee.dccon.exception.UserNotFoundException;
import org.bremersee.dccon.model.DnsEntry;
import org.bremersee.dccon.model.DnsRecord;
import org.bremersee.dccon.model.DnsRecordType;
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.model.DomainGroupItem;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.model.Name;
import org.bremersee.dccon.model.Names;
import org.bremersee.dccon.model.Password;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * This domain controller connector service stores the data in memory.
 *
 * @author Christian Bremer
 */
@SuppressWarnings("Duplicates")
@Profile("in-memory")
@Component
@Slf4j
public class InMemoryDomainControllerConnectorService implements DomainControllerConnectorService {

  private Map<String, DomainGroup> domainGroupMap = new ConcurrentHashMap<>();

  private Map<String, DomainUser> domainUserMap = new ConcurrentHashMap<>();

  private Map<DnsZone, List<DnsEntry>> dnsMap = new ConcurrentHashMap<>();

  @PostConstruct
  public void init() {
    final DomainUser admin = new DomainUser();
    admin.setCreated(OffsetDateTime.now());
    admin.setDisplayName("Super User");
    admin.setDistinguishedName("cn=admin,cn=users,dc=example,dc=org");
    admin.setEmail("admin@example.org");
    admin.setEnabled(true);
    admin.setGroups(Arrays.asList(
        new Name("ADMIN", false),
        new Name("USER", false)));
    admin.setModified(admin.getCreated());
    admin.setPassword("changeit");
    admin.setPasswordLastSet(admin.getCreated());
    admin.setUserName("admin");
    addUser(admin);

    final DomainGroup adminGroup = new DomainGroup();
    adminGroup.setCreated(OffsetDateTime.now());
    adminGroup.setDistinguishedName("cn=admin,cn=groups,dc=example,dc=org");
    //noinspection ArraysAsListWithZeroOrOneArgument
    adminGroup.setMembers(Arrays.asList(new Name("admin", false)));
    adminGroup.setModified(adminGroup.getCreated());
    adminGroup.setName("ADMIN");
    addGroup(adminGroup);

    final DomainGroup userGroup = new DomainGroup();
    userGroup.setCreated(OffsetDateTime.now());
    userGroup.setDistinguishedName("cn=admin,cn=groups,dc=example,dc=org");
    //noinspection ArraysAsListWithZeroOrOneArgument
    userGroup.setMembers(Arrays.asList(new Name("admin", false)));
    userGroup.setModified(userGroup.getCreated());
    userGroup.setName("USER");
    addGroup(userGroup);
  }

  @Override
  public List<DomainGroupItem> getGroups() {
    log.info("msg=[Getting domain groups]");
    return new ArrayList<>(domainGroupMap.values());
  }

  @Override
  public DomainGroup addGroup(@Valid DomainGroup group) {

    log.info("msg=[Adding domain group] group=[{}]", group);
    try {
      getGroupByName(group.getName());
      throw new GroupAlreadyExistsException(group.getName());

    } catch (final NotFoundException nfe) {

      domainGroupMap.put(group.getName(), group);
      return group;
    }
  }

  @Override
  public DomainGroup getGroupByName(@NotNull String groupName) {
    log.info("msg=[Getting domain group by name] name=[{}]", groupName);
    final DomainGroup domainGroup = domainGroupMap.get(groupName);
    if (domainGroup == null) {
      throw GroupNotFoundException.supplier(groupName).get();
    }
    return domainGroup;
  }

  @Override
  public DomainGroup updateGroupMembers(@NotNull String groupName, @Valid Names members) {
    log.info("msg=[Updating domain group members] group=[{}] members=[{}]", groupName, members);
    final DomainGroup domainGroup = getGroupByName(groupName);
    domainGroup.getMembers().clear();
    domainGroup.getMembers().addAll(members.getValues());
    return domainGroup;
  }

  @Override
  public void deleteGroup(@NotNull String groupName) {
    log.info("msg=[Deleting domain group.] group=[{}]", groupName);
    domainGroupMap.remove(groupName);
  }

  @Override
  public DomainUser addUser(@Valid DomainUser domainUser) {

    log.info("msg=[Adding domain user.] user=[{}]", domainUser);
    domainUserMap.put(domainUser.getUserName(), domainUser);
    log.info("msg=[Domain user successfully added.] user=[{}]", domainUser);
    return domainUser;
  }

  @Override
  public boolean userExists(@NotNull String userName) {
    return domainUserMap.containsKey(userName);
  }

  @Override
  public DomainUser getUser(@NotNull String userName) {
    final DomainUser domainUser = domainUserMap.get(userName);
    if (domainUser == null) {
      throw UserNotFoundException.supplier(userName).get();
    }
    return domainUser;
  }

  @Override
  public DomainUser updateUser(@NotNull String userName, @Valid DomainUser domainUser) {
    if (!domainUserMap.containsKey(userName)) {
      throw UserNotFoundException.supplier(userName).get();
    }
    domainUserMap.put(userName, domainUser);
    return domainUser;
  }

  @Override
  public DomainUser updateUserGroups(@NotNull String userName, @Valid Names groups) {
    final DomainUser domainUser = domainUserMap.get(userName);
    if (domainUser == null) {
      throw UserNotFoundException.supplier(userName).get();
    }
    domainUser.getGroups().clear();
    domainUser.getGroups().addAll(groups.getValues());
    return domainUser;
  }

  @Override
  public void updateUserPassword(@NotNull String userName, @Valid Password newPassword) {
    // ignored
  }

  @Override
  public void deleteUser(@NotNull String userName) {
    domainUserMap.remove(userName);
  }

  @Override
  public List<DnsZone> getDnsZones() {
    return new ArrayList<>(dnsMap.keySet());
  }

  @Override
  public List<DnsZone> getDnsReverseZones() {
    return getDnsZones();
  }

  @Override
  public List<DnsZone> getDnsNonReverseZones() {
    return getDnsZones();
  }

  @Override
  public void createDnsZone(@NotNull String zoneName) {
    dnsMap.put(DnsZone.builder().pszZoneName(zoneName).build(), new ArrayList<>());
  }

  @Override
  public void deleteDnsZone(@NotNull String zoneName) {
    dnsMap.remove(DnsZone.builder().pszZoneName(zoneName).build());
  }

  @Override
  public List<DnsEntry> getDnsRecords(@NotNull String zoneName) {
    return dnsMap.getOrDefault(DnsZone.builder().pszZoneName(zoneName).build(), new ArrayList<>());
  }

  @Override
  public boolean dnsRecordExists(
      @NotNull String zoneName,
      @NotNull String name,
      @NotNull DnsRecordType recordType,
      @NotNull String data) {
    return getDnsRecords(zoneName)
        .parallelStream()
        .anyMatch(entry -> entry
            .getName().equals(name)
            && entry.getRecords()
            .parallelStream()
            .anyMatch(record -> recordType.name()
                .equals(record.getRecordType())
                && data.equals(record.getRecordValue())));
  }

  @Override
  public void addDnsRecord(
      @NotNull String zoneName,
      @NotNull String name,
      @NotNull DnsRecordType recordType,
      @NotNull String data) {

    final DnsEntry dnsEntry = new DnsEntry();
    dnsEntry.setName(name);
    final DnsRecord dnsRecord = new DnsRecord();
    dnsRecord.setRecordType(recordType != null ? recordType.name() : null);
    dnsRecord.setRecordValue(data);
    dnsEntry.getRecords().add(dnsRecord);
    final DnsZone dnsZone = DnsZone.builder().pszZoneName(zoneName).build();
    dnsMap
        .computeIfAbsent(dnsZone, zone -> new ArrayList<>())
        .add(dnsEntry);
  }

  @Override
  public void updateDnsRecord(
      @NotNull String zoneName,
      @NotNull String name,
      @NotNull DnsRecordType recordType,
      @NotNull String oldData,
      @NotNull String newData) {

    final DnsZone dnsZone = DnsZone.builder().pszZoneName(zoneName).build();
    final List<DnsEntry> entries = dnsMap.getOrDefault(dnsZone, new ArrayList<>());
    for (DnsEntry entry : entries) {
      if (entry.getName().equalsIgnoreCase(name) && entry.getRecords() != null) {
        for (DnsRecord record : entry.getRecords()) {
          if (record.getRecordValue().equalsIgnoreCase(oldData)) {
            record.setRecordValue(newData);
          }
        }
        break;
      }
    }
  }

  @Override
  public void deleteDnsRecord(
      @NotNull String zoneName,
      @NotNull String name,
      @NotNull DnsRecordType recordType,
      @NotNull String data) {

    final DnsZone dnsZone = DnsZone.builder().pszZoneName(zoneName).build();
    final List<DnsEntry> entries = dnsMap.getOrDefault(dnsZone, new ArrayList<>());
    for (DnsEntry entry : entries) {
      if (entry.getName().equalsIgnoreCase(name) && entry.getRecords() != null) {
        final Iterator<DnsRecord> iter = entry.getRecords().iterator();
        while (iter.hasNext()) {
          DnsRecord record = iter.next();
          if (record.getRecordValue().equalsIgnoreCase(data)) {
            iter.remove();
            break;
          }
        }
        break;
      }
    }
  }
}
