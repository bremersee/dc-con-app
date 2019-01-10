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

package org.bremersee.smbcon.business;

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
import org.bremersee.smbcon.exception.GroupAlreadyExistsException;
import org.bremersee.smbcon.exception.GroupNotFoundException;
import org.bremersee.smbcon.exception.NotFoundException;
import org.bremersee.smbcon.exception.UserNotFoundException;
import org.bremersee.smbcon.model.DnsEntry;
import org.bremersee.smbcon.model.DnsRecord;
import org.bremersee.smbcon.model.DnsRecordType;
import org.bremersee.smbcon.model.DnsZone;
import org.bremersee.smbcon.model.Name;
import org.bremersee.smbcon.model.Names;
import org.bremersee.smbcon.model.Password;
import org.bremersee.smbcon.model.SambaGroup;
import org.bremersee.smbcon.model.SambaGroupItem;
import org.bremersee.smbcon.model.SambaUser;
import org.bremersee.smbcon.model.SambaUserAddRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * This samba connector service stores the data in memory.
 *
 * @author Christian Bremer
 */
@SuppressWarnings("Duplicates")
@Profile("in-memory")
@Component
@Slf4j
public class InMemorySambaConnectorService implements SambaConnectorService {

  private Map<String, SambaGroup> sambaGroupMap = new ConcurrentHashMap<>();

  private Map<String, SambaUser> sambaUserMap = new ConcurrentHashMap<>();

  private Map<DnsZone, List<DnsEntry>> dnsMap = new ConcurrentHashMap<>();

  @PostConstruct
  public void init() {
    final SambaUserAddRequest admin = new SambaUserAddRequest();
    admin.setCreated(OffsetDateTime.now());
    admin.setDisplayName("Super User");
    admin.setDistinguishedName("cn=admin,cn=users,dc=example,dc=org");
    admin.setEmail("admin@example.org");
    admin.setEnabled(true);
    admin.setGroups(Arrays.asList(
        new Name().value("ADMIN").distinguishedName(false),
        new Name().value("USER").distinguishedName(false)));
    admin.setModified(admin.getCreated());
    admin.setPassword("changeit");
    admin.setPasswordLastSet(admin.getCreated());
    admin.setUserName("admin");
    addUser(admin);

    final SambaGroup adminGroup = new SambaGroup();
    adminGroup.setCreated(OffsetDateTime.now());
    adminGroup.setDistinguishedName("cn=admin,cn=groups,dc=example,dc=org");
    //noinspection ArraysAsListWithZeroOrOneArgument
    adminGroup.setMembers(Arrays.asList(new Name().value("admin").distinguishedName(false)));
    adminGroup.setModified(adminGroup.getCreated());
    adminGroup.setName("ADMIN");
    addGroup(adminGroup);

    final SambaGroup userGroup = new SambaGroup();
    userGroup.setCreated(OffsetDateTime.now());
    userGroup.setDistinguishedName("cn=admin,cn=groups,dc=example,dc=org");
    //noinspection ArraysAsListWithZeroOrOneArgument
    userGroup.setMembers(Arrays.asList(new Name().value("admin").distinguishedName(false)));
    userGroup.setModified(userGroup.getCreated());
    userGroup.setName("USER");
    addGroup(userGroup);
  }

  @Override
  public List<SambaGroupItem> getGroups() {
    log.info("msg=[Getting samba groups]");
    return new ArrayList<>(sambaGroupMap.values());
  }

  @Override
  public SambaGroup addGroup(@Valid SambaGroup group) {

    log.info("msg=[Adding samba group] group=[{}]", group);
    try {
      getGroupByName(group.getName());
      throw new GroupAlreadyExistsException(group.getName());

    } catch (final NotFoundException nfe) {

      sambaGroupMap.put(group.getName(), group);
      return group;
    }
  }

  @Override
  public SambaGroup getGroupByName(@NotNull String groupName) {
    log.info("msg=[Getting samba group by name] name=[{}]", groupName);
    final SambaGroup sambaGroup = sambaGroupMap.get(groupName);
    if (sambaGroup == null) {
      throw GroupNotFoundException.supplier(groupName).get();
    }
    return sambaGroup;
  }

  @Override
  public SambaGroup updateGroupMembers(@NotNull String groupName, @Valid Names members) {
    log.info("msg=[Updating samba group members] group=[{}] members=[{}]", groupName, members);
    final SambaGroup sambaGroup = getGroupByName(groupName);
    sambaGroup.getMembers().clear();
    sambaGroup.getMembers().addAll(members.getValues());
    return sambaGroup;
  }

  @Override
  public void deleteGroup(@NotNull String groupName) {
    log.info("msg=[Deleting samba group.] group=[{}]", groupName);
    sambaGroupMap.remove(groupName);
  }

  @Override
  public SambaUser addUser(@Valid SambaUserAddRequest sambaUser) {

    log.info("msg=[Adding samba user.] user=[{}]", sambaUser);
    sambaUserMap.put(sambaUser.getUserName(), sambaUser);
    log.info("msg=[Samba user successfully added.] user=[{}]", sambaUser);
    return sambaUser;
  }

  @Override
  public boolean userExists(@NotNull String userName) {
    return sambaUserMap.containsKey(userName);
  }

  @Override
  public SambaUser getUser(@NotNull String userName) {
    final SambaUser sambaUser = sambaUserMap.get(userName);
    if (sambaUser == null) {
      throw UserNotFoundException.supplier(userName).get();
    }
    return sambaUser;
  }

  @Override
  public SambaUser updateUser(@NotNull String userName, @Valid SambaUser sambaUser) {
    if (!sambaUserMap.containsKey(userName)) {
      throw UserNotFoundException.supplier(userName).get();
    }
    sambaUserMap.put(userName, sambaUser);
    return sambaUser;
  }

  @Override
  public SambaUser updateUserGroups(@NotNull String userName, @Valid Names groups) {
    final SambaUser sambaUser = sambaUserMap.get(userName);
    if (sambaUser == null) {
      throw UserNotFoundException.supplier(userName).get();
    }
    sambaUser.getGroups().clear();
    sambaUser.getGroups().addAll(groups.getValues());
    return sambaUser;
  }

  @Override
  public void updateUserPassword(@NotNull String userName, @Valid Password newPassword) {
    // ignored
  }

  @Override
  public void deleteUser(@NotNull String userName) {
    sambaUserMap.remove(userName);
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
    dnsMap.put(new DnsZone().pszZoneName(zoneName), new ArrayList<>());
  }

  @Override
  public void deleteDnsZone(@NotNull String zoneName) {
    dnsMap.remove(new DnsZone().pszZoneName(zoneName));
  }

  @Override
  public List<DnsEntry> getDnsRecords(@NotNull String zoneName) {
    return dnsMap.getOrDefault(new DnsZone().pszZoneName(zoneName), new ArrayList<>());
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
    dnsEntry.addRecordsItem(dnsRecord);
    final DnsZone dnsZone = new DnsZone().pszZoneName(zoneName);
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

    final DnsZone dnsZone = new DnsZone().pszZoneName(zoneName);
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

    final DnsZone dnsZone = new DnsZone().pszZoneName(zoneName);
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
