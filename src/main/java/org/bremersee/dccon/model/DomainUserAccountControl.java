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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * The domain user's account control.
 *
 * @author Christian Bremer
 */
@Schema(description = "Domain user's account control.")
@ToString
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class DomainUserAccountControl {

  @Schema(
      description = "Specifies whether the domain user is enabled or not.",
      defaultValue = "true")
  @JsonProperty(value = "enabled", defaultValue = "true")
  private Boolean enabled = true;

  @Schema(
      description = "Specifies whether the password expiration is enabled or not.",
      defaultValue = "false")
  @JsonProperty(value = "passwordExpirationEnabled", defaultValue = "false")
  private Boolean passwordExpirationEnabled = false;

  @Builder(toBuilder = true)
  public DomainUserAccountControl(Boolean enabled, Boolean passwordExpirationEnabled) {
    this.enabled = enabled;
    this.passwordExpirationEnabled = passwordExpirationEnabled;
  }

  public Boolean getEnabled() {
    return isNull(enabled) || enabled;
  }

  public Boolean getPasswordExpirationEnabled() {
    return Boolean.TRUE.equals(passwordExpirationEnabled);
  }
}
