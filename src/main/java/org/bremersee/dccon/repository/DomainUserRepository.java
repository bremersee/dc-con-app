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

package org.bremersee.dccon.repository;

import java.util.Optional;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;
import org.bremersee.dccon.model.DomainUser;
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
   * @return the users
   */
  Stream<DomainUser> findAll();

  /**
   * Find user by name.
   *
   * @param userName the user name
   * @return the user
   */
  Optional<DomainUser> findOne(@NotNull String userName);

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
   * @return the domain user
   */
  DomainUser save(@NotNull DomainUser domainUser);

  /**
   * Save password.
   *
   * @param userName    the user name
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
