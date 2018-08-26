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
import javax.validation.constraints.NotNull;
import org.bremersee.smbcon.model.DnsEntry;
import org.bremersee.smbcon.model.DnsRecordType;
import org.bremersee.smbcon.model.DnsZone;

/**
 * @author Christian Bremer
 */
public interface SambaTool {

  void createUser(
      @NotNull String userName,
      @NotNull String password,
      String displayName,
      String email,
      String mobile);

  void deleteUser(@NotNull String userName);

  void setNewPassword(@NotNull String userName, @NotNull String newPassword);


  void addGroup(@NotNull String groupName);

  void deleteGroup(@NotNull String groupName);


  List<DnsZone> getDnsZones();

  void createDnsZone(@NotNull String zoneName);

  void deleteDnsZone(@NotNull String zoneName);

  List<DnsEntry> getDnsRecords(@NotNull String zoneName);

  void addDnsRecord(
      @NotNull String zoneName,
      @NotNull String name,
      @NotNull DnsRecordType recordType,
      @NotNull String data);

  void deleteDnsRecord(
      @NotNull String zoneName,
      @NotNull String name,
      @NotNull DnsRecordType recordType,
      @NotNull String data);

  void updateDnsRecord(
      @NotNull String zoneName,
      @NotNull String name,
      @NotNull DnsRecordType recordType,
      @NotNull String oldData,
      @NotNull String newData);

}
