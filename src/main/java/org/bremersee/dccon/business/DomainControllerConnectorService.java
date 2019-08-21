/*
 * Copyright 2016 the original author or authors.
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

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.bremersee.dccon.model.AddDhcpLeaseParameter;
import org.bremersee.dccon.model.DhcpLease;
import org.bremersee.dccon.model.DnsEntry;
import org.bremersee.dccon.model.DnsRecordType;
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.model.DomainGroupItem;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.model.Password;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;

/**
 * The domain controller connector service interface.
 *
 * @author Christian Bremer
 */
@Validated
public interface DomainControllerConnectorService {

  /**
   * Gets domain groups.
   *
   * @return the groups
   */
  List<DomainGroupItem> getGroups();

  /**
   * Add domain group.
   *
   * @param group the group
   * @return the domain group
   */
  DomainGroup addGroup(@Valid DomainGroup group);

  /**
   * Gets group by name.
   *
   * @param groupName the group name
   * @return the group by name
   */
  DomainGroup getGroupByName(@NotNull String groupName);

  /**
   * Update group members of domain group.
   *
   * @param groupName the group name
   * @param members   the members
   * @return the domain group
   */
  DomainGroup updateGroupMembers(
      @NotNull String groupName,
      @Valid List<String> members);

  /**
   * Delete group.
   *
   * @param groupName the group name
   */
  void deleteGroup(@NotNull String groupName);


  /**
   * Add domain user.
   *
   * @param domainUser the domain user
   * @return the domain user
   */
  DomainUser addUser(@Valid DomainUser domainUser);

  /**
   * Does user exist?
   *
   * @param userName the user name
   * @return the boolean
   */
  boolean userExists(@NotNull String userName);

  /**
   * Gets user.
   *
   * @param userName the user name
   * @return the user
   */
  DomainUser getUser(@NotNull String userName);

  /**
   * Update domain user.
   *
   * @param userName     the user name
   * @param domainUser   the domain user
   * @param updateGroups the update groups (default is false)
   * @return the domain user
   */
  DomainUser updateUser(
      @NotNull String userName,
      @Valid DomainUser domainUser,
      @Nullable Boolean updateGroups);

  /**
   * Update groups of domain user.
   *
   * @param userName the user name
   * @param groups   the groups
   * @return the domain user
   */
  DomainUser updateUserGroups(
      @NotNull String userName,
      @Valid List<String> groups);

  /**
   * Update user password.
   *
   * @param userName    the user name
   * @param newPassword the new password
   */
  void updateUserPassword(@NotNull String userName, @Valid Password newPassword);

  /**
   * Delete user.
   *
   * @param userName the user name
   */
  void deleteUser(@NotNull String userName);


  /**
   * Gets dhcp leases.
   *
   * @param all  if {@code true}, expired leases will also be returned, otherwise only active ones
   * @param sort the sort order (default is {@link DhcpLease#SORT_ORDER_BEGIN_HOSTNAME})
   * @return the dhcp leases
   */
  List<DhcpLease> getDhcpLeases(@Nullable Boolean all, @Nullable String sort);

  /**
   * Gets all dns zones.
   *
   * @return the dns zones
   */
  List<DnsZone> getDnsZones();

  /**
   * Gets all dns reverse zones.
   *
   * @return the dns reverse zones
   */
  List<DnsZone> getDnsReverseZones();

  /**
   * Gets all dns zones which are not reverse ones.
   *
   * @return the dns zones which are not reverse ones
   */
  List<DnsZone> getDnsNonReverseZones();

  /**
   * Create dns zone.
   *
   * @param zoneName the zone name
   */
  void createDnsZone(@NotNull String zoneName);

  /**
   * Delete dns zone.
   *
   * @param zoneName the zone name
   */
  void deleteDnsZone(@NotNull String zoneName);

  /**
   * Gets dns records.
   *
   * @param zoneName     the zone name
   * @param addDhcpLease the add dhcp lease
   * @return the dns records
   */
  List<DnsEntry> getDnsRecords(
      @NotNull String zoneName,
      @NotNull AddDhcpLeaseParameter addDhcpLease);

  /**
   * Checks whether a dns record exists or not.
   *
   * @param zoneName   the zone name
   * @param name       the name
   * @param recordType the record type
   * @param data       the data
   * @return {@code true} if the record exists otherwise {@code false}
   */
  boolean dnsRecordExists(
      @NotNull String zoneName,
      @NotNull String name,
      @NotNull DnsRecordType recordType,
      @NotNull String data);

  /**
   * Adds a dns record.
   *
   * @param zoneName             the zone name
   * @param name                 the name
   * @param recordType           the record type
   * @param data                 the data
   * @param alsoAddReverseRecord also add reverse record flag (default is {@code true}
   */
  void addDnsRecord(
      @NotNull String zoneName,
      @NotNull String name,
      @NotNull DnsRecordType recordType,
      @NotNull String data,
      @Nullable Boolean alsoAddReverseRecord);

  /**
   * Update dns record.
   *
   * @param zoneName   the zone name
   * @param name       the name
   * @param recordType the record type
   * @param oldData    the old data
   * @param newData    the new data
   */
  void updateDnsRecord(
      @NotNull String zoneName,
      @NotNull String name,
      @NotNull DnsRecordType recordType,
      @NotNull String oldData,
      @NotNull String newData);

  /**
   * Delete dns record.
   *
   * @param zoneName   the zone name
   * @param name       the name
   * @param recordType the record type
   * @param data       the data
   * @param alsoDeleteReverseRecord also delete reverse record flag (default is {@code true}
   */
  void deleteDnsRecord(
      @NotNull String zoneName,
      @NotNull String name,
      @NotNull DnsRecordType recordType,
      @NotNull String data,
      @Nullable Boolean alsoDeleteReverseRecord);

}
