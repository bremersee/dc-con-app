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
import javax.validation.constraints.NotNull;
import org.bremersee.dccon.model.DnsEntry;
import org.bremersee.dccon.model.DnsRecordType;
import org.bremersee.dccon.model.DnsZone;

/**
 * The samba tool interface.
 *
 * @author Christian Bremer
 */
public interface SambaTool {

  /**
   * Create user.
   *
   * @param userName        the user name
   * @param password        the password
   * @param firstName       the first name
   * @param lastName        the last name
   * @param displayName     the display name
   * @param email           the email
   * @param telephoneNumber the telephone number
   */
  void createUser(
      @NotNull String userName,
      String password,
      String firstName,
      String lastName,
      String displayName,
      String email,
      String telephoneNumber);

  /**
   * Delete user.
   *
   * @param userName the user name
   */
  void deleteUser(@NotNull String userName);

  /**
   * Sets new password.
   *
   * @param userName    the user name
   * @param newPassword the new password
   */
  void setNewPassword(@NotNull String userName, @NotNull String newPassword);


  /**
   * Add group.
   *
   * @param groupName the group name
   */
  void addGroup(@NotNull String groupName);

  /**
   * Delete group.
   *
   * @param groupName the group name
   */
  void deleteGroup(@NotNull String groupName);


  /**
   * Gets dns zones.
   *
   * @return the dns zones
   */
  List<DnsZone> getDnsZones();

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
   * Add dns record.
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

}
