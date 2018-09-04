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

package org.bremersee.smbcon.controller;

import java.util.List;
import javax.validation.Valid;
import org.bremersee.smbcon.SambaDomainProperties;
import org.bremersee.smbcon.api.SambaConnectorControllerApi;
import org.bremersee.smbcon.business.SambaConnectorService;
import org.bremersee.smbcon.model.BooleanWrapper;
import org.bremersee.smbcon.model.DnsEntry;
import org.bremersee.smbcon.model.DnsRecordRequest;
import org.bremersee.smbcon.model.DnsRecordUpdateRequest;
import org.bremersee.smbcon.model.DnsZone;
import org.bremersee.smbcon.model.DnsZoneCreateRequest;
import org.bremersee.smbcon.model.Info;
import org.bremersee.smbcon.model.Names;
import org.bremersee.smbcon.model.Password;
import org.bremersee.smbcon.model.SambaGroup;
import org.bremersee.smbcon.model.SambaGroupItem;
import org.bremersee.smbcon.model.SambaUser;
import org.bremersee.smbcon.model.SambaUserAddRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The samba connector controller.
 *
 * @author Christian Bremer
 */
@RestController
@SuppressWarnings("MVCPathVariableInspection")
public class SambaConnectorController implements SambaConnectorControllerApi {

  private final SambaDomainProperties sambaDomainProperties;

  private final SambaConnectorService sambaConnectorService;

  @Autowired
  public SambaConnectorController(
      final SambaDomainProperties sambaDomainProperties,
      final SambaConnectorService sambaConnectorService) {
    this.sambaDomainProperties = sambaDomainProperties;
    this.sambaConnectorService = sambaConnectorService;
  }

  @PreAuthorize("#oauth2.hasScope('samba:admin')")
  @Override
  public ResponseEntity<List<DnsZone>> getDnsZones() {
    return ResponseEntity.ok(sambaConnectorService.getDnsZones());
  }

  @PreAuthorize("#oauth2.hasScope('samba:admin')")
  @Override
  public ResponseEntity<List<DnsEntry>> getDnsRecords(
      @Valid @RequestParam(value = "zoneName") String zoneName) {

    return ResponseEntity.ok(sambaConnectorService.getDnsRecords(zoneName));
  }

  @PreAuthorize("#oauth2.hasScope('samba:admin')")
  @Override
  public ResponseEntity<SambaGroup> addGroup(
      @Valid @RequestBody SambaGroup group) {

    return ResponseEntity.ok(sambaConnectorService.addGroup(group));
  }

  @PreAuthorize("#oauth2.hasScope('samba:admin')")
  @Override
  public ResponseEntity<SambaUser> addUser(
      @Valid @RequestBody SambaUserAddRequest sambaUser) {

    return ResponseEntity.ok(sambaConnectorService.addUser(sambaUser));
  }

  @PreAuthorize("#oauth2.hasScope('samba:admin')")
  @Override
  public ResponseEntity<Void> createDnsZone(
      @Valid @RequestBody DnsZoneCreateRequest request) {

    sambaConnectorService.createDnsZone(request.getPszZoneName());
    return ResponseEntity.ok().build();
  }

  @PreAuthorize("#oauth2.hasScope('samba:admin')")
  @Override
  public ResponseEntity<Void> createOrDeleteDnsRecord(
      @Valid @RequestParam(value = "action") final String action,
      @Valid @RequestBody final DnsRecordRequest request) {

    if ("DELETE".equals(action)) {
      sambaConnectorService
          .deleteDnsRecord(request.getZoneName(), request.getName(), request.getRecordType(),
              request.getData());

    } else {
      sambaConnectorService
          .addDnsRecord(request.getZoneName(), request.getName(), request.getRecordType(),
              request.getData());

    }
    return ResponseEntity.ok().build();
  }

  @PreAuthorize("#oauth2.hasScope('samba:admin')")
  @Override
  public ResponseEntity<Void> deleteDnsZone(
      @Valid @RequestParam(value = "zoneName") final String zoneName) {

    sambaConnectorService.deleteDnsZone(zoneName);
    return null;
  }

  @PreAuthorize("#oauth2.hasScope('samba:admin')")
  @Override
  public ResponseEntity<Void> deleteGroup(
      @PathVariable("groupName") String groupName) {

    sambaConnectorService.deleteGroup(groupName);
    return ResponseEntity.ok().build();
  }

  @PreAuthorize("#oauth2.hasScope('samba:admin')")
  @Override
  public ResponseEntity<Void> deleteUser(
      @PathVariable("userName") String userName) {

    sambaConnectorService.deleteUser(userName);
    return ResponseEntity.ok().build();
  }

  @PreAuthorize("#oauth2.hasScope('samba:admin')")
  @Override
  public ResponseEntity<SambaGroup> getGroupByName(
      @PathVariable("groupName") String groupName) {

    return ResponseEntity.ok(sambaConnectorService.getGroupByName(groupName));
  }

  @PreAuthorize("#oauth2.hasScope('samba:admin')")
  @Override
  public ResponseEntity<List<SambaGroupItem>> getGroups() {
    return ResponseEntity.ok(sambaConnectorService.getGroups());
  }

  @PreAuthorize("#oauth2.hasScope('samba:admin')")
  @Override
  public ResponseEntity<Info> getInfo() {
    return ResponseEntity.ok(sambaDomainProperties.buildInfo());
  }

  @PreAuthorize("#oauth2.hasScope('samba:admin')")
  @Override
  public ResponseEntity<BooleanWrapper> userExists(
      @PathVariable("userName") String userName) {

    final BooleanWrapper wrapper = new BooleanWrapper();
    wrapper.setValue(sambaConnectorService.userExists(userName));
    return ResponseEntity.ok(wrapper);
  }

  @PreAuthorize("#oauth2.hasScope('samba:admin')")
  @Override
  public ResponseEntity<SambaUser> getUser(
      @PathVariable("userName") String userName) {

    return ResponseEntity.ok(sambaConnectorService.getUser(userName));
  }

  @PreAuthorize("#oauth2.hasScope('samba:admin')")
  @Override
  public ResponseEntity<Void> updateDnsRecord(@Valid @RequestBody DnsRecordUpdateRequest request) {

    sambaConnectorService.updateDnsRecord(
        request.getZoneName(),
        request.getName(),
        request.getRecordType(),
        request.getOldData(),
        request.getNewData());
    return ResponseEntity.ok().build();
  }

  @PreAuthorize("#oauth2.hasScope('samba:admin')")
  @Override
  public ResponseEntity<SambaGroup> updateGroupMembers(
      @PathVariable("groupName") String groupName,
      @Valid @RequestBody Names members) {

    return ResponseEntity.ok(sambaConnectorService.updateGroupMembers(groupName, members));
  }

  @PreAuthorize("#oauth2.hasScope('samba:admin')")
  @Override
  public ResponseEntity<SambaUser> updateUser(
      @PathVariable("userName") String userName,
      @Valid @RequestBody SambaUser sambaUser) {

    return ResponseEntity.ok(sambaConnectorService.updateUser(userName, sambaUser));
  }

  @PreAuthorize("#oauth2.hasScope('samba:admin')")
  @Override
  public ResponseEntity<SambaUser> updateUserGroups(
      @PathVariable("userName") String userName,
      @Valid @RequestBody Names groups) {

    return ResponseEntity.ok(sambaConnectorService.updateUserGroups(userName, groups));
  }

  @PreAuthorize("#oauth2.hasScope('samba:admin')")
  @Override
  public ResponseEntity<Void> updateUserPassword(
      @PathVariable("userName") String userName,
      @Valid @RequestBody Password newPassword) {

    sambaConnectorService.updateUserPassword(userName, newPassword);
    return ResponseEntity.ok().build();
  }

}
