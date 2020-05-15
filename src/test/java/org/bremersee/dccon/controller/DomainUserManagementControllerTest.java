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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "bremersee.security.authentication.enable-jwt-support=false"
})
@ActiveProfiles({"in-memory"})
class DomainUserManagementControllerTest {

  private static final String user = "admin";

  private static final String pass = "admin";

  @Autowired
  TestRestTemplate restTemplate;

  @BeforeEach
  void setUp() {
    ResponseEntity<Void> response = restTemplate
        .withBasicAuth(user, pass)
        .postForEntity("/api/reset", null, Void.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

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

  @Test
  void getUserAvatar() {
    DomainUser expected = findFirst();
    ResponseEntity<byte[]> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/users/{name}/avatar", byte[].class, expected.getUserName());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

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