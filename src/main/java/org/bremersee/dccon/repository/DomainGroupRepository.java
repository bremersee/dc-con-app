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

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.model.DomainGroupMember;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
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
  Stream<DomainGroup> findAll(
      @Nullable String query,
      @Nullable Dn ou,
      @Nullable SearchScope searchScope);


  Stream<DomainGroup> resolveMembership(
      @NotNull String samAccountName,
      @Nullable Dn ou,
      @Nullable SearchScope searchScope);

  Stream<DomainGroup> getMembership(
      @NotNull String samAccountName,
      @Nullable Dn ou,
      @Nullable SearchScope searchScope);


  Stream<DomainGroupMember> findPossibleMembers(
      @NotNull String groupName,
      @Nullable Dn ou,
      @Nullable SearchScope searchScope);

  Stream<DomainGroupMember> queryPossibleMembers(
      @NotNull String groupName,
      @Nullable Dn ou,
      @Nullable SearchScope searchScope,
      @Nullable String query);

  Stream<DomainGroupMember> getMembers(
      @NotNull String groupName,
      @Nullable Dn ou,
      @Nullable SearchScope searchScope);


  /**
   * Find group be name.
   *
   * @param groupName the group name
   * @return the group
   */
  Optional<DomainGroup> findOne(
      @NotNull String groupName,
      @Nullable Dn ou,
      @Nullable SearchScope searchScope);

  /**
   * Add domain group.
   *
   * @param domainGroup the domain group
   * @return the domain group
   */
  DomainGroup add(@NotNull DomainGroup domainGroup, @Nullable Dn ou);

  /**
   * Update domain group.
   *
   * @param domainGroup the domain group
   * @return the domain group
   */
  @Deprecated
  DomainGroup update(@NotNull DomainGroup domainGroup);

  DomainGroup update(@NotNull String groupName, @NotNull DomainGroup domainGroup, @Nullable Dn newOu);

  /**
   * Delete group.
   *
   * @param groupName the group name
   * @return {@code true} if the group was removed; {@code false} if the group didn't exist
   */
  boolean delete(@NotNull String groupName);

}
