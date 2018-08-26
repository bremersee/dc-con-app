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
 * @author Christian Bremer
 */
public interface SambaConnectorService {

  List<SambaGroupItem> getGroups();

  SambaGroup addGroup(@Valid SambaGroup group);

  SambaGroup getGroupByName(@NotNull String groupName);

  SambaGroup updateGroupMembers(@NotNull String groupName, @Valid Names members);

  void deleteGroup(@NotNull String groupName);


  SambaUser addUser(@Valid SambaUserAddRequest sambaUser);

  boolean userExists(@NotNull String userName);

  SambaUser getUser(@NotNull String userName);

  SambaUser updateUser(@NotNull String userName, @Valid SambaUser sambaUser);

  SambaUser updateUserGroups(@NotNull String userName, @Valid Names groups);

  void updateUserPassword(@NotNull String userName, @Valid Password newPassword);

  void deleteUser(@NotNull String userName);


  List<DnsZone> getDnsZones();

  void createDnsZone(@NotNull String zoneName);

  void deleteDnsZone(@NotNull String zoneName);

  List<DnsEntry> getDnsRecords(@NotNull String zoneName);

  void addDnsRecord(
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

  void deleteDnsRecord(
      @NotNull String zoneName,
      @NotNull String name,
      @NotNull DnsRecordType recordType,
      @NotNull String data);

}
