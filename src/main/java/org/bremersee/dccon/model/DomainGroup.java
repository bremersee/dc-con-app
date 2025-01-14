/*
 * Copyright 2024 the original author or authors.
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

package org.bremersee.dccon.model;

import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.lang.NonNull;

/**
 * A domain (Active Directory) group may contain user and computer accounts as well as other
 * groups.
 *
 * <p>Groups may also be used to establish email distribution lists.
 *
 * <p>See: <a
 * href="https://learn.microsoft.com/en-us/windows-server/identity/ad-ds/manage/understand-security-groups">Active
 * Directory security groups</a>
 *
 * @author Christian Bremer
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class DomainGroup extends SamAccount implements NisDomainMember, Comparable<DomainGroup> {

  /**
   * A description of the domain group.
   */
  private String description;

  /**
   * The email address of the group.
   */
  private String email;

  /**
   * Group's Unix/RFC2307 GID number.
   */
  private Integer gidNumber;

  /**
   * The type of the domain group.
   */
  private DomainGroupTypeContainer groupType;

  /**
   * The members of the domain group.
   */
  private List<String> members;

  /**
   * Group's Unix/RFC2307 NIS domain.
   */
  private String nisDomain;

  /**
   * Returns the type of the domain group.
   *
   * @return the domain group type.
   */
  public DomainGroupTypeContainer getGroupType() {
    if (isNull(groupType) || isNull(groupType.getGroupTypeValue())) {
      return new DomainGroupTypeContainer(DomainGroupType.GLOBAL_SECURITY);
    }
    return groupType;
  }

  /**
   * The members of the domain group.
   *
   * @return the members
   */
  public List<String> getMembers() {
    if (members == null) {
      members = new ArrayList<>();
    }
    return members;
  }

  @Override
  public Integer getPrimaryGroupId() {
    return Optional.ofNullable(getSid())
        .map(Sid::getSuffix)
        .orElse(super.getPrimaryGroupId());
  }

  @Override
  public void setPrimaryGroupId(Integer primaryGroupId) {
    // ignored
  }

  @Override
  public int compareTo(@NonNull DomainGroup o) {
    String s1 = Objects.requireNonNullElse(getSamAccountName(), "");
    String s2 = Objects.requireNonNullElse(o.getSamAccountName(), "");
    return s1.compareToIgnoreCase(s2);
  }
}
