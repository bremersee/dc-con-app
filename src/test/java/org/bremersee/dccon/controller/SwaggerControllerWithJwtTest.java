package org.bremersee.dccon.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "bremersee.security.authentication.enable-jwt-support=true",
    "bremersee.swagger.title=Domain Controller Connector for JUnit"
})
@ActiveProfiles({"default", "test"})
class SwaggerControllerWithJwtTest {

  @Autowired
  TestRestTemplate restTemplate;

  @Test
  void callSwaggerApi() {
    ResponseEntity<String> response = restTemplate
        .getForEntity("/v2/api-docs", String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String actual = response.getBody();
    assertNotNull(actual);
    assertTrue(actual.contains("Domain Controller Connector for JUnit"));
  }

}