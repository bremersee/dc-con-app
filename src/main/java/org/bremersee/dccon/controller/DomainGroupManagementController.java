/*
 * Copyright 2019 the original author or authors.
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
import org.bremersee.dccon.api.DomainGroupManagementApi;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.service.DomainGroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * The domain group management controller.
 *
 * @author Christian Bremer
 */
@RestController
public class DomainGroupManagementController implements DomainGroupManagementApi {

  private final DomainGroupService domainGroupService;

  /**
   * Instantiates a new domain group management controller.
   *
   * @param domainGroupService the domain group service
   */
  public DomainGroupManagementController(
      final DomainGroupService domainGroupService) {
    this.domainGroupService = domainGroupService;
  }

  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LOCAL_USER')")
  @Override
  public ResponseEntity<List<DomainGroup>> getGroups(String sort, String query) {
    return ResponseEntity.ok(domainGroupService.getGroups(sort, query));
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<DomainGroup> addGroup(
      @Valid DomainGroup group) {
    return ResponseEntity.ok(domainGroupService.addGroup(group));
  }

  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LOCAL_USER')")
  @Override
  public ResponseEntity<DomainGroup> getGroup(String groupName) {
    return ResponseEntity.of(domainGroupService.getGroup(groupName));
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<DomainGroup> updateGroup(String groupName, @Valid DomainGroup domainGroup) {
    return ResponseEntity.of(domainGroupService.updateGroup(groupName, domainGroup));
  }

  /*
  public ResponseEntity<DomainGroup> setMembers(String groupName, List<String> userNames) {
    return ResponseEntity.of(domainGroupService.getGroup(groupName)
        .flatMap(group -> {
          group.setMembers(userNames != null ? new ArrayList<>(userNames) : new ArrayList<>());
          return domainGroupService.updateGroup(groupName, group);
        }));
  }

  public ResponseEntity<DomainGroup> addMembers(String groupName, List<String> userNames) {
    return ResponseEntity.of(domainGroupService.getGroup(groupName)
        .flatMap(group -> {
          Set<String> members = new HashSet<>(group.getMembers());
          members.addAll(userNames);
          group.setMembers(new ArrayList<>(members));
          return domainGroupService.updateGroup(groupName, group);
        }));
  }

  public ResponseEntity<DomainGroup> removeMembers(String groupName, List<String> userNames) {
    return ResponseEntity.of(domainGroupService.getGroup(groupName)
        .flatMap(group -> {
          Set<String> members = new HashSet<>(group.getMembers());
          members.removeAll(userNames);
          group.setMembers(new ArrayList<>(members));
          return domainGroupService.updateGroup(groupName, group);
        }));
  }
  */

  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LOCAL_USER')")
  @Override
  public ResponseEntity<Boolean> groupExists(String groupName) {
    return ResponseEntity.ok(domainGroupService.groupExists(groupName));
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<Boolean> deleteGroup(String groupName) {
    return ResponseEntity.ok(domainGroupService.deleteGroup(groupName));
  }

}
