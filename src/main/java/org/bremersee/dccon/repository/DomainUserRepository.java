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

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import java.util.stream.Stream;
import org.bremersee.dccon.model.DomainUser;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;

/**
 * The domain user repository interface.
 *
 * @author Christian Bremer
 */
@Validated
public interface DomainUserRepository extends DomainUserRepositoryConstants, AvatarRepository {

  /**
   * Find all users.
   *
   * @param query the query
   * @return the users
   */
  Stream<DomainUser> findAll(
      @Nullable String query,
      @Nullable Dn ou,
      @Nullable SearchScope searchScope);

  /**
   * Find user by name.
   *
   * @param userName the user name
   * @return the user
   */
  Optional<DomainUser> findOne(
      @NotEmpty String userName,
      @Nullable Dn ou,
      @Nullable SearchScope searchScope);

  /**
   * Add domain user.
   *
   * @param domainUser the domain user
   * @return the domain user
   */
  DomainUser add(@NotNull DomainUser domainUser, @Nullable Dn ou,
      @Nullable Boolean useUsernameAsCn);

  /**
   * Update domain user.
   *
   * @param domainUser the domain user
   * @return the domain user
   */
  @Deprecated
  DomainUser update(@NotNull DomainUser domainUser);

  DomainUser update(@NotEmpty String userName, @NotNull DomainUser domainUser, @Nullable Dn newOu);

  /**
   * Save password.
   *
   * @param userName the user name
   * @param newPassword the new password
   */
  void savePassword(@NotEmpty String userName, @NotEmpty String newPassword);

  /**
   * Delete user.
   *
   * @param userName the user name
   * @return {@code true} if the user was removed; {@code false} if the user didn't exist
   */
  boolean delete(@NotEmpty String userName);

}
