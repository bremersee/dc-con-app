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
import org.bremersee.dccon.api.DomainUserManagementApi;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.model.Password;
import org.bremersee.dccon.service.DomainUserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * The domain user management controller.
 *
 * @author Christian Bremer
 */
@RestController
public class DomainUserManagementController implements DomainUserManagementApi {

  private final DomainUserService domainUserService;

  /**
   * Instantiates a new domain user management controller.
   *
   * @param domainUserService the domain user service
   */
  public DomainUserManagementController(
      final DomainUserService domainUserService) {
    this.domainUserService = domainUserService;
  }

  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LOCAL_USER')")
  @Override
  public ResponseEntity<List<DomainUser>> getUsers(final String sort, String query) {
    return ResponseEntity.ok(domainUserService.getUsers(sort, query));
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<DomainUser> addUser(
      @Valid final DomainUser domainUser) {
    return ResponseEntity.ok(domainUserService.addUser(domainUser));
  }

  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LOCAL_USER')")
  @Override
  public ResponseEntity<DomainUser> getUser(
      final String userName,
      final Boolean addAvailableGroups) {
    return ResponseEntity.of(domainUserService.getUser(userName, addAvailableGroups));
  }

  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LOCAL_USER')")
  @Override
  public ResponseEntity<byte[]> getUserAvatar(
      final String userName,
      final Boolean returnDefault) {

    return domainUserService.getUserAvatar(userName, returnDefault)
        .map(avatar -> ResponseEntity
            .status(HttpStatus.OK)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + userName + ".jpeg\"")
            .body(avatar))
        .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<DomainUser> updateUser(
      final String userName,
      final Boolean updateGroups,
      @Valid final DomainUser domainUser) {
    return ResponseEntity.of(domainUserService.updateUser(userName, updateGroups, domainUser));
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<Void> updateUserPassword(
      final String userName,
      @Valid final Password newPassword) {
    domainUserService.updateUserPassword(userName, newPassword);
    return ResponseEntity.ok().build();
  }

  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LOCAL_USER')")
  @Override
  public ResponseEntity<Boolean> userExists(final String userName) {
    return ResponseEntity.ok(domainUserService.userExists(userName));
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<Boolean> deleteUser(final String userName) {
    return ResponseEntity.ok(domainUserService.deleteUser(userName));
  }

}
