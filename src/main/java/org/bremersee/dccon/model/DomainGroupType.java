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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * The domain group type.
 *
 * <p>See: <a
 * href="https://learn.microsoft.com/en-us/windows-server/identity/ad-ds/manage/understand-security-groups">Active
 * Directory security groups</a>
 *
 * @author Christian Bremer
 */
@Getter
public enum DomainGroupType {

  UNKNOWN(null, null, null),

  DOMAIN_LOCAL_SECURITY(Scope.DOMAIN_LOCAL, Purpose.SECURITY, -2147483644),

  DOMAIN_LOCAL_DISTRIBUTION(Scope.DOMAIN_LOCAL, Purpose.DISTRIBUTION, 4),

  GLOBAL_SECURITY(Scope.GLOBAL, Purpose.SECURITY, -2147483646),

  GLOBAL_DISTRIBUTION(Scope.GLOBAL, Purpose.DISTRIBUTION, 2),

  UNIVERSAL_SECURITY(Scope.UNIVERSAL, Purpose.SECURITY, -2147483640),

  UNIVERSAL_DISTRIBUTION(Scope.UNIVERSAL, Purpose.DISTRIBUTION, 8);

  @Getter
  private final Scope scope;

  @Getter
  private final Purpose purpose;

  @JsonIgnore
  private final Integer value;

  DomainGroupType(Scope scope, Purpose purpose, Integer value) {
    this.scope = scope;
    this.purpose = purpose;
    this.value = value;
  }

  @JsonValue
  @Override
  public String toString() {
    return name().toLowerCase();
  }

  @JsonCreator
  public static DomainGroupType fromString(String type) {
    if (isNull(type)) {
      return null;
    }
    try {
      return DomainGroupType.valueOf(type.toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public static DomainGroupType fromValue(Integer value) {
    if (isNull(value)) {
      return UNKNOWN;
    }
    for (DomainGroupType type : DomainGroupType.values()) {
      if (value.equals(type.value)) {
        return type;
      }
    }
    return UNKNOWN;
  }

  public static DomainGroupType fromScopeAndPurpose(Scope scope, Purpose purpose) {
    if (isNull(scope) || isNull(purpose)) {
      return UNKNOWN;
    }
    for (DomainGroupType type : DomainGroupType.values()) {
      if (scope.equals(type.scope) && purpose.equals(type.purpose)) {
        return type;
      }
    }
    return UNKNOWN;
  }

  public enum Scope {
    UNIVERSAL("Universal"),
    GLOBAL("Global"),
    DOMAIN_LOCAL("Domain");

    private final String value;

    Scope(String value) {
      this.value = value;
    }

    @JsonValue
    @Override
    public String toString() {
      return value;
    }

    @JsonCreator
    public static Scope fromString(String scope) {
      if (isNull(scope)) {
        return null;
      }
      for (Scope type : Scope.values()) {
        if (scope.equalsIgnoreCase(type.value) || scope.equalsIgnoreCase(type.name())) {
          return type;
        }
      }
      return null;
    }
  }

  public enum Purpose {
    SECURITY("Security"),
    DISTRIBUTION("Distribution");

    private final String value;

    Purpose(String purpose) {
      this.value = purpose;
    }

    @JsonValue
    @Override
    public String toString() {
      return value;
    }

    @JsonCreator
    public static Purpose fromString(String value) {
      if (isNull(value)) {
        return null;
      }
      for (Purpose purpose : Purpose.values()) {
        if (value.equalsIgnoreCase(purpose.value) || value.equalsIgnoreCase(purpose.name())) {
          return purpose;
        }
      }
      return null;
    }
  }

}
