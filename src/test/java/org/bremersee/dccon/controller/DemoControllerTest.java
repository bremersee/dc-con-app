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

import org.bremersee.exception.model.RestApiException;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
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
  void resetData() {
    ResponseEntity<Void> response = restTemplate
        .withBasicAuth(user, pass)
        .postForEntity("/api/reset", null, Void.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  /**
   * Reset data and expect.
   */
  @Order(2)
  @Test
  void resetDataAndExpect() {
    ResponseEntity<Void> response = restTemplate
        .postForEntity("/api/reset", null, Void.class);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  /**
   * Throw error.
   */
  @Order(3)
  @Test
  void throwError() {
    ResponseEntity<RestApiException> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/error", RestApiException.class);
    assertEquals(HttpStatus.I_AM_A_TEAPOT, response.getStatusCode());
    RestApiException actual = response.getBody();
    assertNotNull(actual);
    assertEquals("teapot", actual.getMessage());
    assertNotNull(actual.getCause());
    assertEquals("Example error message", actual.getCause().getMessage());
  }

}