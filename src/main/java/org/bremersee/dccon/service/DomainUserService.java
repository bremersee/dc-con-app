/*
 * Copyright 2019-2020 the original author or authors.
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

package org.bremersee.dccon.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.InputStream;
import java.util.Optional;
import org.bremersee.dccon.model.AvatarDefault;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.model.Password;
import org.bremersee.dccon.model.TreeSearchScope;
import org.ldaptive.dn.Dn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;

/**
 * The domain user service interface.
 *
 * @author Christian Bremer
 */
@Validated
public interface DomainUserService {

  /**
   * Reset data. This method is only available in a non productive environment.
   */
  void resetData();

  /**
   * Get domain users.
   *
   * @param pageable the pageable
   * @param query the query
   * @return the users
   */
  Page<DomainUser> getUsers(
      @NotNull Pageable pageable,
      @Nullable String query,
      @Nullable Dn ou,
      @Nullable TreeSearchScope searchScope);

  /**
   * Add domain user.
   *
   * @param domainUser the domain user
   * @param sendEmail specifies whether to send an email or not (default is {@code false})
   * @return the domain user
   */
  DomainUser addUser(
      @NotNull @Valid DomainUser domainUser,
      @Nullable Dn ou,
      @Nullable Boolean useUsernameAsCn,
      @Nullable Boolean sendEmail);

  /**
   * Get domain user.
   *
   * @param userName the user name
   * @return the domain user
   */
  Optional<DomainUser> getUser(@NotEmpty String userName, @Nullable Dn ou,
      @Nullable TreeSearchScope searchScope);

  /**
   * Gets user avatar.
   *
   * @param userName the user name
   * @param avatarDefault the avatar default
   * @param size the size
   * @return the user avatar
   */
  Optional<byte[]> getUserAvatar(
      @NotEmpty String userName,
      @Nullable Dn ou,
      @Nullable TreeSearchScope searchScope,
      @Nullable AvatarDefault avatarDefault,
      @Nullable Integer size);

  /**
   * Update domain user.
   *
   * @param userName the user name
   * @param domainUser the domain user
   * @return the domain user
   */
  @Deprecated
  Optional<DomainUser> updateUser(
      @NotEmpty String userName,
      @NotNull @Valid DomainUser domainUser);

  DomainUser updateUser(
      @NotEmpty String userName,
      @NotNull DomainUser domainUser,
      @Nullable Dn newOu);

  /**
   * Update user password.
   *
   * @param userName the username
   * @param newPassword the new password
   * @param sendEmail specifies whether to send an email or not
   */
  void updateUserPassword(
      @NotEmpty String userName,
      String newPassword,
      boolean sendEmail);

  /**
   * Update user password.
   *
   * @param userName the username
   * @param newPassword the new password
   * @param sendEmail specifies whether to send an email or not (default is {@code false})
   */
  void updateUserPassword(
      @NotEmpty String userName,
      @NotNull @Valid Password newPassword,
      @Nullable Boolean sendEmail);

  /**
   * Update user avatar.
   *
   * @param userName the user name
   * @param avatar the avatar
   */
  void updateUserAvatar(@NotEmpty String userName, @NotNull InputStream avatar);

  /**
   * Remove user avatar.
   *
   * @param userName the user name
   */
  void removeUserAvatar(@NotEmpty String userName);

  /**
   * Delete user.
   *
   * @param userName the user name
   * @return {@code true} if the user was removed; {@code false} if the user didn't exist
   */
  Boolean deleteUser(@NotEmpty String userName);

  boolean existsAvatarInActiveDirectory(
      @NotEmpty String user,
      @Nullable Dn ou,
      @Nullable TreeSearchScope searchScope);

}
