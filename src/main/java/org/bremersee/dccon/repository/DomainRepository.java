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

import static org.bremersee.dccon.config.DomainControllerProperties.getComplexPasswordRegex;

import java.util.regex.Pattern;
import org.apache.commons.lang.RandomStringUtils;
import org.bremersee.dccon.model.PasswordInformation;

/**
 * The domain repository interface.
 *
 * @author Christian Bremer
 */
public interface DomainRepository {

  /**
   * Gets password information.
   *
   * @return the password information
   */
  PasswordInformation getPasswordInformation();

  /**
   * Create random password.
   *
   * @return the random password
   */
  default String createRandomPassword() {
    final char[] source = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz".toCharArray();
    final PasswordInformation passwordInformation = getPasswordInformation();
    final int length;
    if (passwordInformation != null
        && passwordInformation.getMinimumPasswordLength() != null
        && passwordInformation.getMinimumPasswordLength() > 12) {
      length = passwordInformation.getMinimumPasswordLength();
    } else {
      length = 12;
    }
    final Pattern pattern = Pattern.compile(getComplexPasswordRegex(length));
    String pw = RandomStringUtils.random(length, source);
    while (!pattern.matcher(pw).matches()) {
      pw = RandomStringUtils.random(length, source);
    }
    return pw;
  }

}
