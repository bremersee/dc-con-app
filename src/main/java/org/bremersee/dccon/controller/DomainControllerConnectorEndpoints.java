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

package org.bremersee.dccon.controller;

import java.util.List;
import javax.validation.Valid;
import org.bremersee.dccon.api.DomainControllerConnectorApi;
import org.bremersee.dccon.business.DomainControllerConnectorService;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.BooleanWrapper;
import org.bremersee.dccon.model.DnsEntry;
import org.bremersee.dccon.model.DnsRecordRequest;
import org.bremersee.dccon.model.DnsRecordUpdateRequest;
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.model.DnsZoneCreateRequest;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.model.DomainGroupItem;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.model.DomainUserAddRequest;
import org.bremersee.dccon.model.Info;
import org.bremersee.dccon.model.Names;
import org.bremersee.dccon.model.Password;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The domain controller connector endpoints.
 *
 * @author Christian Bremer
 */
@RestController
@SuppressWarnings("MVCPathVariableInspection")
public class DomainControllerConnectorEndpoints implements DomainControllerConnectorApi {

  private final DomainControllerProperties domainControllerProperties;

  private final DomainControllerConnectorService domainControllerConnectorService;

  @Autowired
  public DomainControllerConnectorEndpoints(
      final DomainControllerProperties domainControllerProperties,
      final DomainControllerConnectorService domainControllerConnectorService) {
    this.domainControllerProperties = domainControllerProperties;
    this.domainControllerConnectorService = domainControllerConnectorService;
  }

  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LOCAL_USER')")
  @Override
  public ResponseEntity<List<DnsZone>> getDnsZones() {
    return ResponseEntity.ok(domainControllerConnectorService.getDnsZones());
  }

  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LOCAL_USER')")
  @Override
  public ResponseEntity<List<DnsEntry>> getDnsRecords(
      @Valid @RequestParam(value = "zoneName") String zoneName) {

    return ResponseEntity.ok(domainControllerConnectorService.getDnsRecords(zoneName));
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<DomainGroup> addGroup(
      @Valid @RequestBody DomainGroup group) {

    return ResponseEntity.ok(domainControllerConnectorService.addGroup(group));
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<DomainUser> addUser(
      @Valid @RequestBody DomainUserAddRequest domainUser) {

    return ResponseEntity.ok(domainControllerConnectorService.addUser(domainUser));
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<Void> createDnsZone(
      @Valid @RequestBody DnsZoneCreateRequest request) {

    domainControllerConnectorService.createDnsZone(request.getPszZoneName());
    return ResponseEntity.ok().build();
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<Void> createOrDeleteDnsRecord(
      @Valid @RequestParam(value = "action") final String action,
      @Valid @RequestBody final DnsRecordRequest request) {

    if ("DELETE".equals(action)) {
      domainControllerConnectorService
          .deleteDnsRecord(request.getZoneName(), request.getName(), request.getRecordType(),
              request.getData());

    } else {
      domainControllerConnectorService
          .addDnsRecord(request.getZoneName(), request.getName(), request.getRecordType(),
              request.getData());

    }
    return ResponseEntity.ok().build();
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<Void> deleteDnsZone(
      @Valid @RequestParam(value = "zoneName") final String zoneName) {

    domainControllerConnectorService.deleteDnsZone(zoneName);
    return null;
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<Void> deleteGroup(
      @PathVariable("groupName") String groupName) {

    domainControllerConnectorService.deleteGroup(groupName);
    return ResponseEntity.ok().build();
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<Void> deleteUser(
      @PathVariable("userName") String userName) {

    domainControllerConnectorService.deleteUser(userName);
    return ResponseEntity.ok().build();
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<DomainGroup> getGroupByName(
      @PathVariable("groupName") String groupName) {

    return ResponseEntity.ok(domainControllerConnectorService.getGroupByName(groupName));
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<List<DomainGroupItem>> getGroups() {
    return ResponseEntity.ok(domainControllerConnectorService.getGroups());
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<Info> getInfo() {
    return ResponseEntity.ok(domainControllerProperties.buildInfo());
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<BooleanWrapper> userExists(
      @PathVariable("userName") String userName) {

    final BooleanWrapper wrapper = new BooleanWrapper();
    wrapper.setValue(domainControllerConnectorService.userExists(userName));
    return ResponseEntity.ok(wrapper);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<DomainUser> getUser(
      @PathVariable("userName") String userName) {

    return ResponseEntity.ok(domainControllerConnectorService.getUser(userName));
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<Void> updateDnsRecord(@Valid @RequestBody DnsRecordUpdateRequest request) {

    domainControllerConnectorService.updateDnsRecord(
        request.getZoneName(),
        request.getName(),
        request.getRecordType(),
        request.getOldData(),
        request.getNewData());
    return ResponseEntity.ok().build();
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<DomainGroup> updateGroupMembers(
      @PathVariable("groupName") String groupName,
      @Valid @RequestBody Names members) {

    return ResponseEntity
        .ok(domainControllerConnectorService.updateGroupMembers(groupName, members));
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<DomainUser> updateUser(
      @PathVariable("userName") String userName,
      @Valid @RequestBody DomainUser domainUser) {

    return ResponseEntity.ok(domainControllerConnectorService.updateUser(userName, domainUser));
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<DomainUser> updateUserGroups(
      @PathVariable("userName") String userName,
      @Valid @RequestBody Names groups) {

    return ResponseEntity.ok(domainControllerConnectorService.updateUserGroups(userName, groups));
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<Void> updateUserPassword(
      @PathVariable("userName") String userName,
      @Valid @RequestBody Password newPassword) {

    domainControllerConnectorService.updateUserPassword(userName, newPassword);
    return ResponseEntity.ok().build();
  }

}
