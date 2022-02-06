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

package org.bremersee.dccon.controller;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.bremersee.dccon.model.Password;
import org.bremersee.dccon.model.PasswordInformation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * The domain management controller test.
 *
 * @author Christian Bremer
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"in-memory"})
@ExtendWith(SoftAssertionsExtension.class)
class DomainManagementControllerTest {

  private static final String user = "admin";

  private static final String pass = "admin";

  /**
   * The rest template.
   */
  @Autowired
  TestRestTemplate restTemplate;

  /**
   * Gets password information.
   *
   * @param softly the softly
   */
  @Test
  void getPasswordInformation(SoftAssertions softly) {
    ResponseEntity<PasswordInformation> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/domain/password-information", PasswordInformation.class);
    softly.assertThat(response.getStatusCode())
        .as("Get password information and expect, that status is 200")
        .isEqualTo(HttpStatus.OK);
    PasswordInformation actual = response.getBody();
    softly.assertThat(actual)
        .as("Get password information and expect, that is is not null")
        .isNotNull()
        .extracting(PasswordInformation::getMinimumPasswordLength)
        .as("Get minimum length of password and expect, that is is not null")
        .isNotNull();
  }

  /**
   * Gets random password.
   *
   * @param softly the softly
   */
  @Test
  void getRandomPassword(SoftAssertions softly) {
    ResponseEntity<Password> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/domain/random-password", Password.class);
    softly.assertThat(response.getStatusCode())
        .as("Get random password and expect, that status is 200")
        .isEqualTo(HttpStatus.OK);
    Password actual = response.getBody();
    softly.assertThat(actual)
        .as("Get random password and expect, that is is not null")
        .isNotNull()
        .extracting(Password::getValue)
        .as("Get value of random password and expect, that is is not null")
        .isNotNull();
  }
}