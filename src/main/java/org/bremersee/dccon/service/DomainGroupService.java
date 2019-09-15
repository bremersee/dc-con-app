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
import org.bremersee.dccon.model.DomainGroup;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;

/**
 * The domain group service interface.
 *
 * @author Christian Bremer
 */
@Validated
public interface DomainGroupService {

  /**
   * Get groups.
   *
   * @param sort the sort order
   * @return the groups
   */
  List<DomainGroup> getGroups(@Nullable String sort);

  /**
   * Add domain group.
   *
   * @param domainGroup the domain group
   * @return the domain group
   */
  DomainGroup addGroup(@NotNull @Valid DomainGroup domainGroup);

  /**
   * Get group by name.
   *
   * @param groupName the group name
   * @return the group
   */
  Optional<DomainGroup> getGroupByName(@NotNull String groupName);

  /**
   * Update domain group.
   *
   * @param groupName   the group name
   * @param domainGroup the domain group
   * @return the domain group
   */
  Optional<DomainGroup> updateGroup(@NotNull String groupName, @NotNull @Valid DomainGroup domainGroup);

  /**
   * Check whether group exists or not.
   *
   * @param groupName the group name
   * @return {@code true} if the group exists, otherwise {@code false}
   */
  Boolean groupExists(@NotNull String groupName);

  /**
   * Delete group.
   *
   * @param groupName the group name
   * @return {@code true} if the group was removed; {@code false} if the group didn't exist
   */
  Boolean deleteGroup(@NotNull String groupName);

}
