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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"in-memory"})
class DomainManagementControllerTest {

  private static final String user = "admin";

  private static final String pass = "admin";

  @Autowired
  TestRestTemplate restTemplate;

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