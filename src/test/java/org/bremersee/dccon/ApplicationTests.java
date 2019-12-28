package org.bremersee.dccon;

import java.util.Locale;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.model.DomainUser;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * The application tests.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "basic-auth"})
public class ApplicationTests {

  @Autowired
  private DomainControllerProperties domainControllerProperties;

  /**
   * The Rest template.
   */
  @Autowired
  TestRestTemplate restTemplate;

  /**
   * The Template engine.
   */
  @Autowired
  TemplateEngine templateEngine;

  /**
   * Mail with credentials test.
   */
  @Test
  public void mailWithCredentialsTest() {
    final DomainUser domainUser = DomainUser.builder()
        .displayName("Anna Livia Plurabelle")
        .firstName("Anna")
        .email("anna@example.org")
        .userName("anna")
        .password("changeit")
        .build();
    final Locale locale = Locale.ENGLISH;
    final Context ctx = new Context(locale);
    ctx.setVariable("user", domainUser);
    ctx.setVariable("props", domainControllerProperties);
    ctx.setVariable("lang", locale.getLanguage());
    final String mailText = templateEngine.process(
        domainControllerProperties.getMailWithCredentials().getTemplateBasename(),
        ctx);
    System.out.println("Mail text:\n" + mailText);
  }

  /**
   * Access tests.
   */
  @Test
  public void accessTests() {
    HttpStatus httpStatus = restTemplate
        .getForEntity("/api/info", String.class).getStatusCode();
    Assert.assertEquals(HttpStatus.UNAUTHORIZED, httpStatus);

    httpStatus = restTemplate
        .getForEntity("/actuator/beans", String.class).getStatusCode();
    Assert.assertEquals(HttpStatus.UNAUTHORIZED, httpStatus);

    httpStatus = restTemplate
        .getForEntity("/actuator/health", String.class).getStatusCode();
    Assert.assertEquals(HttpStatus.OK, httpStatus);

    httpStatus = restTemplate
        .getForEntity("/v2/api-docs", String.class).getStatusCode();
    Assert.assertEquals(HttpStatus.OK, httpStatus);

    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setBasicAuth("admin", "admin");
    HttpEntity<?> httpEntity = new HttpEntity<>(null, httpHeaders);
    httpStatus = restTemplate
        .exchange("/api/users/admin", HttpMethod.GET, httpEntity, String.class)
        .getStatusCode();
    Assert.assertEquals(HttpStatus.NOT_FOUND, httpStatus);

    httpHeaders = new HttpHeaders();
    httpHeaders.setBasicAuth("admin", "admin");
    httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
    httpEntity = new HttpEntity<>(DomainGroup.builder().name("test").build(), httpHeaders);
    httpStatus = restTemplate
        .exchange("/api/groups", HttpMethod.POST, httpEntity, String.class)
        .getStatusCode();
    Assert.assertEquals(HttpStatus.OK, httpStatus);

    httpHeaders = new HttpHeaders();
    httpHeaders.setBasicAuth("user", "user");
    httpEntity = new HttpEntity<>(null, httpHeaders);
    httpStatus = restTemplate
        .exchange("/api/groups", HttpMethod.GET, httpEntity, String.class)
        .getStatusCode();
    Assert.assertEquals(HttpStatus.OK, httpStatus);

    httpHeaders = new HttpHeaders();
    httpHeaders.setBasicAuth("user", "user");
    httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
    httpEntity = new HttpEntity<>(DomainGroup.builder().name("test").build(), httpHeaders);
    httpStatus = restTemplate
        .exchange("/api/groups", HttpMethod.POST, httpEntity, String.class)
        .getStatusCode();
    Assert.assertEquals(HttpStatus.FORBIDDEN, httpStatus);

    httpHeaders = new HttpHeaders();
    httpHeaders.setBasicAuth("actuator", "actuator");
    httpEntity = new HttpEntity<>(null, httpHeaders);
    httpStatus = restTemplate
        .exchange("/actuator/beans", HttpMethod.GET, httpEntity, String.class)
        .getStatusCode();
    Assert.assertEquals(HttpStatus.OK, httpStatus);

    httpHeaders = new HttpHeaders();
    httpHeaders.setBasicAuth("user", "user");
    httpEntity = new HttpEntity<>(null, httpHeaders);
    httpStatus = restTemplate
        .exchange("/actuator/conditions", HttpMethod.GET, httpEntity, String.class)
        .getStatusCode();
    Assert.assertEquals(HttpStatus.FORBIDDEN, httpStatus);
  }

}
