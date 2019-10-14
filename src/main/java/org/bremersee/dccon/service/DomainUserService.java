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

package org.bremersee.dccon.service;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.bremersee.dccon.model.AvatarDefault;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.model.Password;
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
   * Get domain users.
   *
   * @param sort  the sort order
   * @param query the query
   * @return the users
   */
  List<DomainUser> getUsers(@Nullable String sort, @Nullable String query);

  /**
   * Add domain user.
   *
   * @param domainUser the domain user
   * @return the domain user
   */
  DomainUser addUser(@NotNull @Valid DomainUser domainUser);

  /**
   * Get domain user.
   *
   * @param userName the user name
   * @return the domain user
   */
  Optional<DomainUser> getUser(@NotNull String userName);

  /**
   * Update domain user.
   *
   * @param userName     the user name
   * @param updateGroups specifies whether the groups should also be updated or not (default is
   *                     false)
   * @param domainUser   the domain user
   * @return the domain user
   */
  Optional<DomainUser> updateUser(
      @NotNull String userName,
      @Nullable Boolean updateGroups,
      @NotNull @Valid DomainUser domainUser);

  /**
   * Update user password.
   *
   * @param userName    the user name
   * @param newPassword the new password
   */
  void updateUserPassword(
      @NotNull String userName,
      @NotNull @Valid Password newPassword);

  /**
   * Gets user avatar.
   *
   * @param userName      the user name
   * @param avatarDefault the avatar default
   * @param size          the size
   * @return the user avatar
   */
  Optional<byte[]> getUserAvatar(
      @NotNull String userName,
      @Nullable AvatarDefault avatarDefault,
      @Nullable Integer size);

  /**
   * Check whether user exists or not.
   *
   * @param userName the user name
   * @return {@code true} if the user exists, otherwise {@code false}
   */
  Boolean userExists(@NotNull String userName);

  /**
   * Delete user.
   *
   * @param userName the user name
   * @return {@code true} if the user was removed; {@code false} if the user didn't exist
   */
  Boolean deleteUser(@NotNull String userName);

}
