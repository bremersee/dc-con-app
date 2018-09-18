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

package org.bremersee.smbcon.business;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.bremersee.smbcon.model.DnsEntry;
import org.bremersee.smbcon.model.DnsRecordType;
import org.bremersee.smbcon.model.DnsZone;
import org.bremersee.smbcon.model.Names;
import org.bremersee.smbcon.model.Password;
import org.bremersee.smbcon.model.SambaGroup;
import org.bremersee.smbcon.model.SambaGroupItem;
import org.bremersee.smbcon.model.SambaUser;
import org.bremersee.smbcon.model.SambaUserAddRequest;

/**
 * The samba connector service interface.
 *
 * @author Christian Bremer
 */
public interface SambaConnectorService {

  /**
   * Gets samba groups.
   *
   * @return the groups
   */
  List<SambaGroupItem> getGroups();

  /**
   * Add samba group.
   *
   * @param group the group
   * @return the samba group
   */
  SambaGroup addGroup(@Valid SambaGroup group);

  /**
   * Gets group by name.
   *
   * @param groupName the group name
   * @return the group by name
   */
  SambaGroup getGroupByName(@NotNull String groupName);

  /**
   * Update group members of samba group.
   *
   * @param groupName the group name
   * @param members   the members
   * @return the samba group
   */
  SambaGroup updateGroupMembers(@NotNull String groupName, @Valid Names members);

  /**
   * Delete group.
   *
   * @param groupName the group name
   */
  void deleteGroup(@NotNull String groupName);


  /**
   * Add samba user.
   *
   * @param sambaUser the samba user
   * @return the samba user
   */
  SambaUser addUser(@Valid SambaUserAddRequest sambaUser);

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
  SambaUser getUser(@NotNull String userName);

  /**
   * Update samba user.
   *
   * @param userName  the user name
   * @param sambaUser the samba user
   * @return the samba user
   */
  SambaUser updateUser(@NotNull String userName, @Valid SambaUser sambaUser);

  /**
   * Update groups of samba user.
   *
   * @param userName the user name
   * @param groups   the groups
   * @return the samba user
   */
  SambaUser updateUserGroups(@NotNull String userName, @Valid Names groups);

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
   * @param zoneName the zone name
   * @return the dns records
   */
  List<DnsEntry> getDnsRecords(@NotNull String zoneName);

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
   * @param zoneName   the zone name
   * @param name       the name
   * @param recordType the record type
   * @param data       the data
   */
  void addDnsRecord(
      @NotNull String zoneName,
      @NotNull String name,
      @NotNull DnsRecordType recordType,
      @NotNull String data);

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
   */
  void deleteDnsRecord(
      @NotNull String zoneName,
      @NotNull String name,
      @NotNull DnsRecordType recordType,
      @NotNull String data);

}
