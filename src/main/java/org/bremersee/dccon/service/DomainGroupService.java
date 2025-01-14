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
import java.util.Optional;
import java.util.stream.Stream;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.model.DomainGroupMembers;
import org.bremersee.dccon.model.TreeSearchScope;
import org.ldaptive.dn.Dn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
   * @param pageable the pageable
   * @param query the query
   * @return the groups
   */
  Page<DomainGroup> getGroups(
      @NotNull Pageable pageable,
      @Nullable String query,
      @Nullable Dn ou,
      @Nullable TreeSearchScope searchScope);


  Stream<DomainGroup> resolveMemberships(
      @NotEmpty String samAccountName,
      @Nullable Dn ou,
      @Nullable TreeSearchScope searchScope);

  Stream<DomainGroup> getMemberships(
      @NotEmpty String samAccountName,
      @Nullable Dn ou,
      @Nullable TreeSearchScope searchScope);


  DomainGroupMembers findPossibleMembers(
      @NotEmpty String groupName,
      @Nullable Dn ou,
      @Nullable TreeSearchScope searchScope);

  DomainGroupMembers queryPossibleMembers(
      @NotEmpty String groupName,
      @Nullable Dn ou,
      @Nullable TreeSearchScope searchScope,
      @Nullable String query);

  DomainGroupMembers getMembers(
      @NotEmpty String groupName,
      @Nullable Dn ou,
      @Nullable TreeSearchScope searchScope);


  /**
   * Add domain group.
   *
   * @param domainGroup the domain group
   * @return the domain group
   */
  DomainGroup addGroup(@NotNull @Valid DomainGroup domainGroup, @Nullable Dn ou);

  /**
   * Get group by name.
   *
   * @param groupName the group name
   * @return the group
   */
  Optional<DomainGroup> getGroup(
      @NotEmpty String groupName,
      @Nullable Dn ou,
      @Nullable TreeSearchScope searchScope);

  /**
   * Update domain group.
   *
   * @param groupName the group name
   * @param domainGroup the domain group
   * @return the domain group
   */
  @Deprecated
  Optional<DomainGroup> updateGroup(
      @NotEmpty String groupName,
      @NotNull @Valid DomainGroup domainGroup);

  DomainGroup updateGroup(
      @NotEmpty String groupName,
      @NotNull @Valid DomainGroup domainGroup,
      @Nullable Dn newOu);

  /**
   * Delete group.
   *
   * @param groupName the group name
   * @return {@code true} if the group was removed; {@code false} if the group didn't exist
   */
  Boolean deleteGroup(@NotEmpty String groupName);

}
