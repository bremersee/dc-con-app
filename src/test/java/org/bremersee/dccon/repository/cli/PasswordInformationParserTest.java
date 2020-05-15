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

package org.bremersee.dccon.repository.cli;

import static org.bremersee.dccon.repository.cli.PasswordInformationParser.Default.ACCOUNT_LOCKOUT_DURATION;
import static org.bremersee.dccon.repository.cli.PasswordInformationParser.Default.ACCOUNT_LOCKOUT_THRESHOLD;
import static org.bremersee.dccon.repository.cli.PasswordInformationParser.Default.MAXIMUM_PASSWORD_AGE;
import static org.bremersee.dccon.repository.cli.PasswordInformationParser.Default.MINIMUM_PASSWORD_AGE;
import static org.bremersee.dccon.repository.cli.PasswordInformationParser.Default.MINIMUM_PASSWORD_LENGTH;
import static org.bremersee.dccon.repository.cli.PasswordInformationParser.Default.PASSWORD_COMPLEXITY;
import static org.bremersee.dccon.repository.cli.PasswordInformationParser.Default.PASSWORD_HISTORY_LENGTH;
import static org.bremersee.dccon.repository.cli.PasswordInformationParser.Default.RESET_ACCOUNT_LOCKOUT_AFTER;
import static org.bremersee.dccon.repository.cli.PasswordInformationParser.Default.STORE_PLAINTEXT_PASSWORD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.bremersee.dccon.model.PasswordComplexity;
import org.bremersee.dccon.model.PasswordInformation;
import org.junit.jupiter.api.Test;

/**
 * The password information parser test.
 *
 * @author Christian Bremer
 */
class PasswordInformationParserTest {

  private static final PasswordInformationParser parser = PasswordInformationParser.defaultParser();

  /**
   * Parse empty password information.
   */
  @Test
  void parseEmptyPasswordInformation() {
    CommandExecutorResponse response = new CommandExecutorResponse(null, null);
    PasswordInformation expected = new PasswordInformation();
    PasswordInformation actual = parser.parse(response);
    assertNotNull(actual);
    assertEquals(expected, actual);
  }

  /**
   * Parse password information.
   */
  @Test
  void parsePasswordInformation() {
    String stdout = PASSWORD_COMPLEXITY + " on\n"
        + STORE_PLAINTEXT_PASSWORD + " false\n"
        + PASSWORD_HISTORY_LENGTH + " 1234\n"
        + MINIMUM_PASSWORD_LENGTH + " 5678\n"
        + MINIMUM_PASSWORD_AGE + " 9012\n"
        + MAXIMUM_PASSWORD_AGE + " 3456\n"
        + ACCOUNT_LOCKOUT_DURATION + " 7890\n"
        + ACCOUNT_LOCKOUT_THRESHOLD + " 12345\n"
        + RESET_ACCOUNT_LOCKOUT_AFTER + " 67890\n";
    CommandExecutorResponse response = new CommandExecutorResponse(stdout, null);
    PasswordInformation expected = PasswordInformation.builder()
        .passwordComplexity(PasswordComplexity.ON)
        .storePlaintextPasswords(false)
        .passwordHistoryLength(1234)
        .minimumPasswordLength(5678)
        .minimumPasswordAgeInDays(9012)
        .maximumPasswordAgeInDays(3456)
        .accountLockoutDurationInMinutes(7890)
        .accountLockoutThreshold(12345)
        .resetAccountLockoutAfter(67890)
        .build();
    PasswordInformation actual = parser.parse(response);
    assertNotNull(actual);
    assertEquals(expected, actual);
  }
}