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

package org.bremersee.dccon.repository;

import java.io.InputStream;
import java.util.Optional;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;
import org.bremersee.dccon.model.AvatarDefault;
import org.bremersee.dccon.model.DomainUser;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;

/**
 * The domain user repository interface.
 *
 * @author Christian Bremer
 */
@Validated
public interface DomainUserRepository {

  /**
   * Find all users.
   *
   * @param query the query
   * @return the users
   */
  Stream<DomainUser> findAll(@Nullable String query);

  /**
   * Find user by name.
   *
   * @param userName the user name
   * @return the user
   */
  Optional<DomainUser> findOne(@NotNull String userName);

  /**
   * Find avatar.
   *
   * @param userName the user name
   * @param avatarDefault the avatar default
   * @param size the size
   * @return the avatar
   */
  Optional<byte[]> findAvatar(
      @NotNull String userName,
      @Nullable AvatarDefault avatarDefault,
      @Nullable Integer size);

  /**
   * Save avatar.
   *
   * @param userName the user name
   * @param avatar the avatar
   */
  void saveAvatar(@NotNull String userName, @NotNull InputStream avatar);

  /**
   * Remove avatar.
   *
   * @param userName the user name
   */
  void removeAvatar(@NotNull String userName);

  /**
   * Check whether user exists or not.
   *
   * @param userName the user name
   * @return {@code true} if the user exists, otherwise {@code false}
   */
  boolean exists(@NotNull String userName);

  /**
   * Save domain user.
   *
   * @param domainUser the domain user
   * @param updateGroups specifies whether the groups should also be updated or not (default is
   *     false)
   * @return the domain user
   */
  DomainUser save(@NotNull DomainUser domainUser, Boolean updateGroups);

  /**
   * Save password.
   *
   * @param userName the user name
   * @param newPassword the new password
   */
  void savePassword(@NotNull String userName, @NotNull String newPassword);

  /**
   * Delete user.
   *
   * @param userName the user name
   * @return {@code true} if the user was removed; {@code false} if the user didn't exist
   */
  boolean delete(@NotNull String userName);

}
