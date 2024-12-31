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

package org.bremersee.dccon.repository.transcoder;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.bremersee.dccon.model.DomainUserAccountControl;

/**
 * The domain user's account control.
 *
 * @author Christian Bremer
 */
@EqualsAndHashCode
@ToString
@Getter
public class UserAccountControl {

  /**
   * The bit map value of a disabled account.
   */
  static final int DISABLED_ACCOUNT = 1 << 1;

  /**
   * The bit map value of a normal account.
   */
  static final int NORMAL_ACCOUNT = 1 << 9;

  /**
   * The bit map value of a password that doesn't expire.
   */
  static final int DONT_EXPIRE_PASSWORD = 1 << 16;

  private int value;

  public UserAccountControl() {
    this(null);
  }

  public UserAccountControl(Integer value) {
    if (isNull(value) || value <= 0) {
      this.value = NORMAL_ACCOUNT;
      setEnabled(true);
      setPasswordExpirationEnabled(false);
    } else {
      this.value = value;
    }
  }

  public UserAccountControl(Integer value, DomainUserAccountControl domainUserAccountControl) {
    if (isNull(value) || value <= 0) {
      this.value = NORMAL_ACCOUNT;
      setEnabled(true);
      setPasswordExpirationEnabled(false);
    } else {
      this.value = value;
    }
    if (nonNull(domainUserAccountControl)) {
      setEnabled(domainUserAccountControl.getEnabled());
      setPasswordExpirationEnabled(domainUserAccountControl.getPasswordExpirationEnabled());
    }
  }

  public boolean isEnabled() {
    return (value & DISABLED_ACCOUNT) != DISABLED_ACCOUNT;
  }

  public void setEnabled(boolean enabled) {
    if (enabled && !isEnabled()) {
      value = value - DISABLED_ACCOUNT;
    } else if (!enabled && isEnabled()) {
      value = value + DISABLED_ACCOUNT;
    }
  }

  public boolean isPasswordExpirationEnabled() {
    return (value & DONT_EXPIRE_PASSWORD) != DONT_EXPIRE_PASSWORD;
  }

  public void setPasswordExpirationEnabled(boolean enabled) {
    if (enabled && !isPasswordExpirationEnabled()) {
      value = value - DONT_EXPIRE_PASSWORD;
    } else if (!enabled && isPasswordExpirationEnabled()) {
      value = value + DONT_EXPIRE_PASSWORD;
    }
  }

  public DomainUserAccountControl toDomainUserAccountControl() {
    return DomainUserAccountControl.builder()
        .enabled(isEnabled())
        .passwordExpirationEnabled(isPasswordExpirationEnabled())
        .build();
  }

}
