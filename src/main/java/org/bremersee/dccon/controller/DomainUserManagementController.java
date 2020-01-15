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

import static org.bremersee.security.core.AuthorityConstants.ADMIN_ROLE_NAME;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import org.bremersee.common.model.TwoLetterLanguageCode;
import org.bremersee.dccon.api.DomainUserManagementApi;
import org.bremersee.dccon.model.AvatarDefault;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.model.Password;
import org.bremersee.dccon.service.AuthenticationService;
import org.bremersee.dccon.service.DomainGroupService;
import org.bremersee.dccon.service.DomainUserService;
import org.bremersee.exception.ServiceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

/**
 * The domain user management controller.
 *
 * @author Christian Bremer
 */
@RestController
public class DomainUserManagementController implements DomainUserManagementApi {

  private final DomainUserService domainUserService;

  private final DomainGroupService domainGroupService;

  private final AuthenticationService authenticationService;

  /**
   * Instantiates a new domain user management controller.
   *
   * @param domainUserService the domain user service
   * @param domainGroupService the domain group service
   * @param authenticationService the authentication service
   */
  public DomainUserManagementController(
      final DomainUserService domainUserService,
      final DomainGroupService domainGroupService,
      final AuthenticationService authenticationService) {
    this.domainUserService = domainUserService;
    this.domainGroupService = domainGroupService;
    this.authenticationService = authenticationService;
  }

  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DC_CON_ADMIN', 'ROLE_LOCAL_USER')")
  @Override
  public ResponseEntity<List<DomainUser>> getUsers(final String sort, String query) {
    return ResponseEntity.ok(domainUserService.getUsers(sort, query));
  }

  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DC_CON_ADMIN')")
  @Override
  public ResponseEntity<DomainUser> addUser(
      final Boolean email,
      final TwoLetterLanguageCode language,
      final DomainUser domainUser) {
    return ResponseEntity.ok(domainUserService.addUser(domainUser, email, language));
  }

  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DC_CON_ADMIN', 'ROLE_LOCAL_USER')")
  @Override
  public ResponseEntity<DomainUser> getUser(
      final String userName) {
    return ResponseEntity.of(domainUserService.getUser(userName));
  }

  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DC_CON_ADMIN', 'ROLE_LOCAL_USER')")
  @Override
  public ResponseEntity<byte[]> getUserAvatar(
      final String userName,
      final AvatarDefault avatarDefault,
      final Integer size) {

    return domainUserService.getUserAvatar(userName, avatarDefault, size)
        .map(avatar -> ResponseEntity
            .status(HttpStatus.OK)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + userName + ".jpg\"")
            .body(avatar))
        .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
  }

  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DC_CON_ADMIN')")
  @Override
  public ResponseEntity<DomainUser> updateUser(
      final String userName,
      final Boolean updateGroups,
      @Valid final DomainUser domainUser) {
    return ResponseEntity.of(domainUserService.updateUser(userName, updateGroups, domainUser));
  }

  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DC_CON_ADMIN', 'ROLE_LOCAL_USER')")
  @Override
  public ResponseEntity<Void> updateUserPassword(
      final String userName,
      final Boolean email,
      final TwoLetterLanguageCode language,
      @Valid final Password newPassword) {

    final boolean sendEmail;
    if (!isAdmin()) {
      sendEmail = Boolean.FALSE;
      if (!isUser(userName)) {
        throw ServiceException.forbidden();
      } else if (!authenticationService.passwordMatches(userName, newPassword.getPreviousValue())) {
        throw ServiceException.badRequest(
            "Previous password does not match.",
            "password_does_not_match");
      }
    } else {
      sendEmail = Boolean.TRUE.equals(email);
    }
    domainUserService.updateUserPassword(userName, newPassword, sendEmail, language);
    return ResponseEntity.ok().build();
  }

  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DC_CON_ADMIN', 'ROLE_LOCAL_USER')")
  @Override
  public ResponseEntity<Boolean> userExists(final String userName) {
    return ResponseEntity.ok(domainUserService.userExists(userName));
  }

  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DC_CON_ADMIN', 'ROLE_LOCAL_USER')")
  @Override
  public ResponseEntity<Boolean> isUserNameInUse(String userName) {
    return ResponseEntity.ok(domainUserService.userExists(userName)
        || domainGroupService.groupExists(userName));
  }

  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DC_CON_ADMIN')")
  @Override
  public ResponseEntity<Boolean> deleteUser(final String userName) {
    return ResponseEntity.ok(domainUserService.deleteUser(userName));
  }

  private boolean isAdmin() {
    return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
        .map(Authentication::getAuthorities)
        .map(authorities -> authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(roleName -> ADMIN_ROLE_NAME
                .equalsIgnoreCase(roleName) || "ROLE_DC_CON_ADMIN".equalsIgnoreCase(roleName)))
        .orElse(false);
  }

  private boolean isUser(String userName) {
    return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
        .map(Authentication::getName)
        .map(name -> name.equalsIgnoreCase(userName))
        .orElse(false);
  }

}
