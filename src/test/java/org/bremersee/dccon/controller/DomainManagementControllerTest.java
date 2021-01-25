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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.bremersee.dccon.model.Password;
import org.bremersee.dccon.model.PasswordInformation;
import org.junit.jupiter.api.Test;
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
   */
  @Test
  void getPasswordInformation() {
    ResponseEntity<PasswordInformation> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/domain/password-information", PasswordInformation.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PasswordInformation actual = response.getBody();
    assertNotNull(actual);
    assertNotNull(actual.getMinimumPasswordLength());
  }

  /**
   * Gets random password.
   */
  @Test
  void getRandomPassword() {
    ResponseEntity<Password> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/domain/random-password", Password.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    Password actual = response.getBody();
    assertNotNull(actual);
    assertNotNull(actual.getValue());
  }
}