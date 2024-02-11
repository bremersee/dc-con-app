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

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.bremersee.exception.model.RestApiException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * The demo controller test.
 *
 * @author Christian Bremer
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"in-memory"})
@TestInstance(Lifecycle.PER_CLASS) // allows us to use @BeforeAll with a non-static method
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(SoftAssertionsExtension.class)
class DemoControllerTest {

  private static final String user = "admin";

  private static final String pass = "admin";

  /**
   * The rest template.
   */
  @Autowired
  TestRestTemplate restTemplate;

  /**
   * Reset data.
   */
  @Order(1)
  @Test
  @Disabled
  void resetData() {
    ResponseEntity<Void> response = restTemplate
        .withBasicAuth(user, pass)
        .postForEntity("/api/reset", null, Void.class);
    assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.OK);
  }

  /**
   * Reset data and expect.
   */
  @Order(2)
  @Test
  @Disabled
  void resetDataAndExpect() {
    ResponseEntity<Void> response = restTemplate
        .postForEntity("/api/reset", null, Void.class);
    assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  /**
   * Throw error.
   *
   * @param softly the softly
   */
  @Order(3)
  @Test
  @Disabled
  void throwError(SoftAssertions softly) {
    ResponseEntity<RestApiException> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/error", RestApiException.class);
    softly.assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.I_AM_A_TEAPOT);
    RestApiException actual = response.getBody();
    softly.assertThat(actual)
        .isNotNull()
        .extracting(RestApiException::getMessage)
        .isEqualTo("teapot");
    softly.assertThat(actual)
        .isNotNull()
        .extracting(RestApiException::getCause)
        .isNotNull()
        .extracting(RestApiException::getMessage)
        .isEqualTo("Example error message");
  }

}