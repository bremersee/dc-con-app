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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "bremersee.security.authentication.enable-jwt-support=false"
})
@ActiveProfiles({"basic-auth"})
@TestInstance(Lifecycle.PER_CLASS) // allows us to use @BeforeAll with a non-static method
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DemoControllerTest {

  private static final String user = "admin";

  private static final String pass = "admin";

  @Autowired
  TestRestTemplate restTemplate;

  @Order(1)
  @Test
  void resetData() {
    ResponseEntity<Void> response = restTemplate
        .withBasicAuth(user, pass)
        .postForEntity("/api/reset", null, Void.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Order(2)
  @Test
  void resetDataAndExpect() {
    ResponseEntity<Void> response = restTemplate
        .postForEntity("/api/reset", null, Void.class);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

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