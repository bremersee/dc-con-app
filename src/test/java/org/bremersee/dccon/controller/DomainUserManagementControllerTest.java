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
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.model.Password;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;

/**
 * The domain user management controller test.
 *
 * @author Christian Bremer
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "bremersee.security.authentication.enable-jwt-support=false"
})
@ActiveProfiles({"in-memory"})
class DomainUserManagementControllerTest {

  private static final String user = "admin";

  private static final String pass = "admin";

  /**
   * The Rest template.
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

  // TODO
  /**
   * Gets users.
   *
  @Test
  void getUsers() {
    ResponseEntity<DomainUser[]> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/users", DomainUser[].class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    DomainUser[] actual = response.getBody();
    assertNotNull(actual);
    assertTrue(actual.length > 0);
  }
  */

  /**
   * Add user.
   */
  @Test
  void addUser() {
    DomainUser source = DomainUser.builder()
        .userName(UUID.randomUUID().toString())
        .description("A new test user.")
        .build();
    ResponseEntity<DomainUser> response = restTemplate
        .withBasicAuth(user, pass)
        .postForEntity("/api/users", source, DomainUser.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    DomainUser actual = response.getBody();
    assertNotNull(actual);
    assertEquals(source.getUserName(), actual.getUserName());
    assertEquals(source.getDescription(), actual.getDescription());
  }

  /**
   * Gets user.
   *
  @Test
  void getUser() {
    DomainUser expected = findFirst();
    ResponseEntity<DomainUser> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/users/{name}", DomainUser.class, expected.getUserName());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    DomainUser actual = response.getBody();
    assertNotNull(actual);
    assertEquals(expected.getUserName(), actual.getUserName());
  }

  /**
   * Gets user avatar.
   *
  @Test
  void getUserAvatar() {
    DomainUser expected = findFirst();
    ResponseEntity<byte[]> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/users/{name}/avatar", byte[].class, expected.getUserName());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  /**
   * Update user.
   *
  @Test
  void updateUser() {
    DomainUser expected = findFirst().toBuilder()
        .description("New test user description")
        .build();
    ResponseEntity<DomainUser> response = restTemplate
        .withBasicAuth(user, pass)
        .exchange("/api/users/{name}",
            HttpMethod.PUT,
            new HttpEntity<>(expected),
            DomainUser.class,
            expected.getUserName());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    DomainUser actual = response.getBody();
    assertNotNull(actual);
    assertEquals(expected.getDescription(), actual.getDescription());
  }

  /**
   * Update user password.
   *
  @Test
  void updateUserPassword() {
    String userName = findFirst().getUserName();
    ResponseEntity<Void> response = restTemplate
        .withBasicAuth(user, pass)
        .exchange("/api/users/{name}/password",
            HttpMethod.PUT,
            new HttpEntity<>(Password.builder().value(UUID.randomUUID().toString()).build()),
            Void.class,
            userName);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  /**
   * Update user password and expect forbidden.
   *
  @Test
  void updateUserPasswordAndExpectForbidden() {
    String userName = findFirst().getUserName();
    ResponseEntity<Void> response = restTemplate
        .withBasicAuth("user", "user")
        .exchange("/api/users/{name}/password",
            HttpMethod.PUT,
            new HttpEntity<>(Password.builder()
                .value(UUID.randomUUID().toString())
                .previousValue(UUID.randomUUID().toString())
                .build()),
            Void.class,
            userName);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  /**
   * Update and remove user avatar.
   *
  @Test
  void updateAndRemoveUserAvatar() {
    LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("avatar", new ClassPathResource("avatar.jpeg"));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    HttpEntity<LinkedMultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

    String userName = findFirst().getUserName();
    ResponseEntity<Void> response = restTemplate
        .withBasicAuth(user, pass)
        .exchange("/api/users/{name}/avatar",
            HttpMethod.POST,
            entity,
            Void.class,
            userName);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    ResponseEntity<byte[]> avatarResponse = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/users/{name}/avatar", byte[].class, userName);
    byte[] actualBytes = avatarResponse.getBody();
    assertNotNull(actualBytes);

    restTemplate
        .withBasicAuth(user, pass)
        .delete("/api/users/{name}/avatar", userName);

    avatarResponse = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/users/{name}/avatar", byte[].class, userName);
    assertEquals(HttpStatus.NOT_FOUND, avatarResponse.getStatusCode());
  }

  /**
   * User exists.
   *
  @Test
  void userExists() {
    DomainUser expected = findFirst();
    ResponseEntity<Boolean> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/users/{name}/exists", Boolean.class, expected.getUserName());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    Boolean actual = response.getBody();
    assertNotNull(actual);
    assertTrue(actual);

    response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/users/{name}/exists", Boolean.class, UUID.randomUUID().toString());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    actual = response.getBody();
    assertNotNull(actual);
    assertFalse(actual);
  }

  /**
   * Is user name in use.
   *
  @Test
  void isUserNameInUse() {
    DomainUser expected = findFirst();
    ResponseEntity<Boolean> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/users/{name}/in-use", Boolean.class, expected.getUserName());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    Boolean actual = response.getBody();
    assertNotNull(actual);
    assertTrue(actual);

    response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/users/{name}/in-use", Boolean.class, UUID.randomUUID().toString());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    actual = response.getBody();
    assertNotNull(actual);
    assertFalse(actual);
  }

  /**
   * Delete user.
   *
  @Test
  void deleteUser() {
    DomainUser expected = findFirst();
    ResponseEntity<Boolean> response = restTemplate
        .withBasicAuth(user, pass)
        .exchange("/api/users/{name}",
            HttpMethod.DELETE,
            null,
            Boolean.class,
            expected.getUserName());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    Boolean actual = response.getBody();
    assertNotNull(actual);
    assertTrue(actual);

    response = restTemplate
        .withBasicAuth(user, pass)
        .exchange("/api/users/{name}",
            HttpMethod.DELETE,
            null,
            Boolean.class,
            UUID.randomUUID().toString());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    actual = response.getBody();
    assertNotNull(actual);
    assertFalse(actual);
  }
  */

  private DomainUser findFirst() {
    ResponseEntity<DomainUser[]> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/users", DomainUser[].class);
    DomainUser[] actual = response.getBody();
    assertNotNull(actual);
    assertTrue(actual.length > 0);
    return actual[0];
  }

}