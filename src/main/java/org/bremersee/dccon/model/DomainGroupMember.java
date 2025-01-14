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

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.lang.NonNull;

/**
 * The domain group member.
 *
 * @author Christian Bremer
 */
@Schema(description = "A member of a domain group.")
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class DomainGroupMember extends SamAccount implements Comparable<DomainGroupMember> {

  @Hidden
  @JsonIgnore
  private String distinguishedNameBase64;

  @Schema(description = "The type of the member.")
  @JsonProperty(value = "memberType", defaultValue = "unknown")
  private DomainGroupMemberType memberType;

  @Schema(description = "The display name of the member.")
  @JsonProperty("displayName")
  private String displayName;

  private boolean selected;

  public DomainGroupMember(
      String distinguishedName,
      OffsetDateTime created,
      OffsetDateTime modified,
      String samAccountName,
      Sid sid,
      Integer primaryGroupId,
      List<String> memberships,
      DomainGroupMemberType objectClass,
      String displayName,
      boolean selected) {
    super(distinguishedName, created, modified, samAccountName, sid, primaryGroupId, memberships);
    this.distinguishedNameBase64 = Base64.getEncoder()
        .encodeToString(getDistinguishedName().getBytes(StandardCharsets.UTF_8));
    this.memberType = requireNonNullElse(objectClass, DomainGroupMemberType.UNKNOWN);
    this.displayName = displayName;
    this.selected = selected;
  }

  public DomainGroupMember(DomainUser domainUser, boolean selected) {
    this(
        domainUser.getDistinguishedName(),
        domainUser.getCreated(),
        domainUser.getModified(),
        domainUser.getSamAccountName(),
        domainUser.getSid(),
        domainUser.getPrimaryGroupId(),
        domainUser.getMemberships(),
        DomainGroupMemberType.USER,
        domainUser.getName(),
        selected);
  }

  public DomainGroupMember(DomainGroup domainGroup, boolean selected) {
    this(
        domainGroup.getDistinguishedName(),
        domainGroup.getCreated(),
        domainGroup.getModified(),
        domainGroup.getSamAccountName(),
        domainGroup.getSid(),
        domainGroup.getPrimaryGroupId(),
        domainGroup.getMemberships(),
        DomainGroupMemberType.GROUP,
        domainGroup.getName(),
        selected);
  }

  // TODO add computer

  @Override
  public void setDistinguishedName(String distinguishedName) {
    super.setDistinguishedName(distinguishedName);
    if (nonNull(distinguishedName)) {
      this.distinguishedNameBase64 = Base64.getEncoder()
          .encodeToString(distinguishedName.getBytes(StandardCharsets.UTF_8));
    } else {
      this.distinguishedNameBase64 = null;
    }
  }

  @Hidden
  @JsonIgnore
  public String getDistinguishedNameBase64() {
    return distinguishedNameBase64;
  }

  @Override
  public int compareTo(@NonNull DomainGroupMember selectOption) {
    String s0 = requireNonNullElse(getDisplayName(), "");
    String s1 = requireNonNullElse(selectOption.getDisplayName(), "");
    int c = s0.compareTo(s1);
    if (c != 0) {
      return c;
    }
    s0 = requireNonNullElse(getSamAccountName(), "");
    s1 = requireNonNullElse(selectOption.getSamAccountName(), "");
    c = s0.compareTo(s1);
    if (c != 0) {
      return c;
    }
    s0 = requireNonNullElse(getSamAccountName(), "");
    s1 = requireNonNullElse(selectOption.getSamAccountName(), "");
    return s0.compareTo(s1);
  }
}
