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

import static java.util.Objects.requireNonNullElse;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A domain (Active Directory) group may contain user and computer accounts as well as other
 * groups.
 *
 * <p>Groups may also be used to establish email distribution lists, using group type
 * {@link DomainGroupType#DISTRIBUTION}.
 *
 * <p>This main representation has a members attribute and a membership attribute with the
 * distinguished names of the referenced entities.
 *
 * @author Christian Bremer
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class DomainGroup extends CommonAttributes
    implements SamAccount, NameProvider, NisDomainMember {

  @Serial
  private static final long serialVersionUID = 2L;

  /**
   * A description of the domain group.
   */
  private String description;

  /**
   * The email address of the domain user.
   */
  private String email;

  /**
   * Group's Unix/RFC2307 GID number.
   */
  private Integer gidNumber;

  /**
   * The type of the domain group.
   */
  private DomainGroupType groupType;

  /**
   * The members of the domain group.
   */
  private List<String> members;

  /**
   * The group's group membership.
   */
  private List<String> membership;

  /**
   * Group's Unix/RFC2307 NIS domain.
   */
  private String nisDomain;

  /**
   * The group's name.
   */
  private String samAccountName;

  /**
   * Group's windows/samba SID.
   */
  private Sid sid;

  /**
   * Returns the type of the domain group.
   *
   * @return the domain group type.
   */
  public DomainGroupType getGroupType() {
    return requireNonNullElse(groupType, DomainGroupType.SECURITY);
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

  /**
   * The group's group membership.
   *
   * @return the group membership
   */
  public List<String> getMembership() {
    if (membership == null) {
      membership = new ArrayList<>();
    }
    return membership;
  }

  public Integer getGroupId() {
    return Optional.ofNullable(getSid())
        .map(Sid::getValue)
        .map(value -> {
          int index = value.lastIndexOf('-');
          if (index == -1) {
            return null;
          }
          try {
            return Integer.parseInt(value.substring(index + 1));
          } catch (RuntimeException e) {
            return null;
          }
        })
        .orElse(null);
  }

}
