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
import org.bremersee.dccon.model.DomainGroup;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;

/**
 * The domain group repository interface.
 *
 * @author Christian Bremer
 */
@Validated
public interface DomainGroupRepository {

  /**
   * Find all groups.
   *
   * @param query the query
   * @return the groups
   */
  Stream<DomainGroup> findAll(@Nullable String query);

  /**
   * Find group be name.
   *
   * @param groupName the group name
   * @return the group
   */
  Optional<DomainGroup> findOne(@NotNull String groupName);

  /**
   * Check whether group exists or not.
   *
   * @param groupName the group name
   * @return {@code true} if the group exists, otherwise {@code false}
   */
  boolean exists(@NotNull String groupName);

  /**
   * Save domain group.
   *
   * @param domainGroup the domain group
   * @return the domain group
   */
  DomainGroup save(@NotNull DomainGroup domainGroup);

  /**
   * Delete group.
   *
   * @param groupName the group name
   * @return {@code true} if the group was removed; {@code false} if the group didn't exist
   */
  boolean delete(@NotNull String groupName);

}
