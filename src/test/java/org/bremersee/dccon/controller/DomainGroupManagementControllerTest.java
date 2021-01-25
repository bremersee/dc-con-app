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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.bremersee.dccon.model.DomainGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * The domain group management controller test.
 *
 * @author Christian Bremer
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"in-memory"})
class DomainGroupManagementControllerTest {

  private static final String user = "admin";

  private static final String pass = "admin";

  /**
   * The rest template.
   */
  @Autowired
  TestRestTemplate restTemplate;

  /**
   * Sets up.
   */
  @BeforeEach
  void setUp() {
    ResponseEntity<Void> response = restTemplate
        .withBasicAuth(user, pass)
        .postForEntity("/api/reset", null, Void.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  /**
   * Gets groups.
   */
  @Test
  void getGroups() {
    ResponseEntity<DomainGroup[]> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/groups", DomainGroup[].class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    DomainGroup[] actual = response.getBody();
    assertNotNull(actual);
    assertTrue(actual.length > 0);
  }

  /**
   * Add group.
   */
  @Test
  void addGroup() {
    DomainGroup source = DomainGroup.builder()
        .name(UUID.randomUUID().toString())
        .description("A new test group.")
        .build();
    ResponseEntity<DomainGroup> response = restTemplate
        .withBasicAuth(user, pass)
        .postForEntity("/api/groups", source, DomainGroup.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    DomainGroup actual = response.getBody();
    assertNotNull(actual);
    assertEquals(source.getName(), actual.getName());
    assertEquals(source.getDescription(), actual.getDescription());
  }

  /**
   * Gets group.
   */
  @Test
  void getGroup() {
    DomainGroup expected = findFirst();
    ResponseEntity<DomainGroup> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/groups/{name}", DomainGroup.class, expected.getName());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    DomainGroup actual = response.getBody();
    assertNotNull(actual);
    assertEquals(expected, actual);
  }

  /**
   * Update group.
   */
  @Test
  void updateGroup() {
    DomainGroup expected = findFirst().toBuilder()
        .description("New test group description")
        .build();
    ResponseEntity<DomainGroup> response = restTemplate
        .withBasicAuth(user, pass)
        .exchange("/api/groups/{name}",
            HttpMethod.PUT,
            new HttpEntity<>(expected),
            DomainGroup.class,
            expected.getName());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    DomainGroup actual = response.getBody();
    assertNotNull(actual);
    assertEquals(expected.getDescription(), actual.getDescription());
  }

  /**
   * Group exists.
   */
  @Test
  void groupExists() {
    DomainGroup expected = findFirst();
    ResponseEntity<Boolean> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/groups/{name}/exists", Boolean.class, expected.getName());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    Boolean actual = response.getBody();
    assertNotNull(actual);
    assertTrue(actual);

    response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/groups/{name}/exists", Boolean.class, UUID.randomUUID().toString());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    actual = response.getBody();
    assertNotNull(actual);
    assertFalse(actual);
  }

  /**
   * Is group name in use.
   */
  @Test
  void isGroupNameInUse() {
    DomainGroup expected = findFirst();
    ResponseEntity<Boolean> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/groups/{name}/in-use", Boolean.class, expected.getName());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    Boolean actual = response.getBody();
    assertNotNull(actual);
    assertTrue(actual);

    response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/groups/{name}/in-use", Boolean.class, UUID.randomUUID().toString());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    actual = response.getBody();
    assertNotNull(actual);
    assertFalse(actual);
  }

  /**
   * Delete group.
   */
  @Test
  void deleteGroup() {
    DomainGroup expected = findFirst();
    ResponseEntity<Boolean> response = restTemplate
        .withBasicAuth(user, pass)
        .exchange("/api/groups/{name}",
            HttpMethod.DELETE,
            null,
            Boolean.class,
            expected.getName());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    Boolean actual = response.getBody();
    assertNotNull(actual);
    assertTrue(actual);

    response = restTemplate
        .withBasicAuth(user, pass)
        .exchange("/api/groups/{name}",
            HttpMethod.DELETE,
            null,
            Boolean.class,
            UUID.randomUUID().toString());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    actual = response.getBody();
    assertNotNull(actual);
    assertFalse(actual);
  }

  private DomainGroup findFirst() {
    ResponseEntity<DomainGroup[]> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/groups", DomainGroup[].class);
    DomainGroup[] actual = response.getBody();
    assertNotNull(actual);
    assertTrue(actual.length > 0);
    return actual[0];
  }
}