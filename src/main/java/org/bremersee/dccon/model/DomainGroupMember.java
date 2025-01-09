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
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.AccessLevel;
import lombok.Builder;
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
@Setter(AccessLevel.PROTECTED)
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DomainGroupMember implements Serializable, Comparable<DomainGroupMember> {

  @Serial
  private static final long serialVersionUID = 1;

  @Schema(description = "The distinguished name of the member.")
  @JsonProperty(value = "distinguishedName", required = true)
  private String distinguishedName;

  @Hidden
  @JsonIgnore
  private String distinguishedNameBase64;

  @Schema(description = "The object class of the member.")
  @JsonProperty(value = "objectClass", defaultValue = "unknown")
  private DomainGroupMemberType objectClass;

  @Schema(description = "The name of the member.")
  @JsonProperty("name")
  private String name;

  @Schema(description = "The display name of the member.")
  @JsonProperty("displayName")
  private String displayName;

  // TODO add organization unit?

  @Builder(toBuilder = true)
  public DomainGroupMember(
      String distinguishedName,
      DomainGroupMemberType objectClass,
      String name,
      String displayName) {
    this.distinguishedName = requireNonNull(distinguishedName, "Distinguished name is required.");
    this.distinguishedNameBase64 = Base64.getEncoder()
        .encodeToString(this.distinguishedName.getBytes(StandardCharsets.UTF_8));
    this.objectClass = requireNonNullElse(objectClass, DomainGroupMemberType.UNKNOWN);
    this.name = requireNonNullElse(name, this.distinguishedName);
    this.displayName = requireNonNullElse(displayName, this.name);
  }

  public DomainGroupMember(DomainUser domainUser) {
    this(
        domainUser.getDistinguishedName(),
        DomainGroupMemberType.USER,
        domainUser.getSamAccountName(),
        domainUser.getName());
  }

  public DomainGroupMember(DomainGroup domainGroup) {
    this(
        domainGroup.getDistinguishedName(),
        DomainGroupMemberType.GROUP,
        domainGroup.getSamAccountName(),
        domainGroup.getName());
  }

  // TODO add computer

  @Hidden
  @JsonIgnore
  public String getDistinguishedNameBase64() {
    return distinguishedNameBase64;
  }

  @Hidden
  @JsonIgnore
  public String getDnWithoutBaseAndSpaces() {
    String dn = getDistinguishedName();
    if (isNull(dn)) {
      return null;
    }
    int index = dn.toLowerCase().indexOf(",dc");
    if (index > 0) {
      dn = dn.substring(0, index);
    }
    return dn.replace(",", ", ");
  }

  @Override
  public int compareTo(@NonNull DomainGroupMember selectOption) {
    String s0 = requireNonNullElse(getDisplayName(), "");
    String s1 = requireNonNullElse(selectOption.getDisplayName(), "");
    int c = s0.compareTo(s1);
    if (c != 0) {
      return c;
    }
    s0 = requireNonNullElse(getName(), "");
    s1 = requireNonNullElse(selectOption.getName(), "");
    c = s0.compareTo(s1);
    if (c != 0) {
      return c;
    }
    s0 = requireNonNullElse(getName(), "");
    s1 = requireNonNullElse(selectOption.getName(), "");
    return s0.compareTo(s1);
  }
}
