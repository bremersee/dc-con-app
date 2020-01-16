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

package org.bremersee.dccon.repository.ldap.transcoder;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.ldaptive.io.AbstractStringValueTranscoder;
import org.springframework.util.StringUtils;

/**
 * The user account control value transcoder.
 *
 * @author Christian Bremer
 */
@Slf4j
public class UserAccountControlValueTranscoder extends AbstractStringValueTranscoder<Integer> {

  /**
   * The bit map value of a disabled account.
   */
  static final int ACCOUNTDISABLE = 1 << 1;

  /**
   * The bit map value of a normal account.
   */
  static final int NORMAL_ACCOUNT = 1 << 9;

  /**
   * The bit map value a password that doesn't expire.
   */
  static final int DONT_EXPIRE_PASSWORD = 1 << 16;

  /**
   * Gets user account control value.
   *
   * @param enabled the enabled
   * @param existingValue the existing value
   * @return the user account control value
   */
  public static int getUserAccountControlValue(Boolean enabled, Integer existingValue) {
    int value = Optional.ofNullable(existingValue).orElse(0);
    if ((value & NORMAL_ACCOUNT) != NORMAL_ACCOUNT) {
      value = value + NORMAL_ACCOUNT;
    }
    if ((value & DONT_EXPIRE_PASSWORD) != DONT_EXPIRE_PASSWORD) {
      value = value + DONT_EXPIRE_PASSWORD;
    }
    if (Boolean.FALSE.equals(enabled)) {
      if ((value & ACCOUNTDISABLE) != ACCOUNTDISABLE) {
        value = value + ACCOUNTDISABLE;
      }
    } else {
      if ((value & ACCOUNTDISABLE) == ACCOUNTDISABLE) {
        value = value - ACCOUNTDISABLE;
      }
    }
    return value;
  }

  /**
   * Is user account enabled boolean.
   *
   * @param userAccountControlValue the user account control value
   * @return the boolean
   */
  public static boolean isUserAccountEnabled(Integer userAccountControlValue) {
    return userAccountControlValue != null
        && ((userAccountControlValue & ACCOUNTDISABLE) != ACCOUNTDISABLE);
  }

  @Override
  public Integer decodeStringValue(String value) {
    return StringUtils.hasText(value)
        ? Integer.parseInt(value)
        : getUserAccountControlValue(true, 0);
  }

  @Override
  public String encodeStringValue(Integer value) {
    return Optional.ofNullable(value)
        .map(String::valueOf)
        .orElseGet(() -> String.valueOf(getUserAccountControlValue(true, 0)));
  }

  @Override
  public Class<Integer> getType() {
    return Integer.class;
  }
}
