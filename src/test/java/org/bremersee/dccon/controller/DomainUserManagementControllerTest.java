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

import java.util.List;
import java.util.UUID;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.bremersee.dccon.model.CommonAttributes;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.model.DomainUserPage;
import org.bremersee.dccon.model.Password;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
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
@ExtendWith(SoftAssertionsExtension.class)
class DomainUserManagementControllerTest {

  private static final String user = "admin";

  private static final String pass = "admin";

  /**
   * The Rest template.
   */
  @Autowired
  TestRestTemplate restTemplate;

  /**
   * The object mapper builder.
   */
  @Autowired
  Jackson2ObjectMapperBuilder objectMapperBuilder;

  /**
   * Sets up.
   */
  @BeforeEach
  void setUp() {
    restTemplate
        .withBasicAuth(user, pass)
        .postForEntity("/api/reset", null, Void.class);
  }

  /**
   * Gets users.
   *
   * @param softly the softly
   */
  @Test
  @Disabled
  void getUsers(SoftAssertions softly) {
    ResponseEntity<DomainUserPage> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/users", DomainUserPage.class);
    softly.assertThat(response.getStatusCode())
        .as("Get users without pageable and expect, that status is 200")
        .isEqualTo(HttpStatus.OK);
    DomainUserPage actual = response.getBody();
    List<DomainUser> expectedContent = findUsers(Sort
        .by(Order.by(DomainUser.USER_NAME).ignoreCase()));
    softly.assertThat(actual)
        .isNotNull()
        .extracting(DomainUserPage::getContent, InstanceOfAssertFactories.list(DomainUser.class))
        .as("Get users without pageable and expect content of users.json")
        .containsExactlyElementsOf(expectedContent);

    response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/users?page=0&size=3&sort=distinguishedName,desc",
            DomainUserPage.class);
    softly.assertThat(response.getStatusCode())
        .as("Get users with pageable and expect status is 200")
        .isEqualTo(HttpStatus.OK);
    actual = response.getBody();
    expectedContent = findUsers(Sort
        .by(Order.by(CommonAttributes.DISTINGUISHED_NAME).ignoreCase().with(Direction.DESC)));
    softly.assertThat(actual)
        .isNotNull()
        .extracting(DomainUserPage::getContent, InstanceOfAssertFactories.list(DomainUser.class))
        .as("Get users with pageable and expect three entries")
        .containsExactlyElementsOf(expectedContent.subList(0, 3));
  }

  /**
   * Add user.
   *
   * @param softly the softly
   */
  @Test
  @Disabled
  void addUser(SoftAssertions softly) {
    DomainUser source = DomainUser.builder()
        .userName(UUID.randomUUID().toString())
        .description("A new test user.")
        .build();
    ResponseEntity<DomainUser> response = restTemplate
        .withBasicAuth(user, pass)
        .postForEntity("/api/users", source, DomainUser.class);
    softly.assertThat(response.getStatusCode())
        .as("Add user and expect, that status is 200")
        .isEqualTo(HttpStatus.OK);
    DomainUser actual = response.getBody();
    softly.assertThat(actual)
        .as("Add user and assert, that response is not null")
        .isNotNull()
        .extracting(DomainUser::getUserName)
        .as("Add user and assert, that user names are equal")
        .isEqualTo(source.getUserName());
    softly.assertThat(actual)
        .isNotNull()
        .as("Add user and assert, that response is not null")
        .extracting(DomainUser::getDescription)
        .as("Add user and assert, that descriptions are equal")
        .isEqualTo(source.getDescription());
  }

  /**
   * Gets user.
   *
   * @param softly the softly
   */
  @Test
  @Disabled
  void getUser(SoftAssertions softly) {
    DomainUser expected = findFirst();
    ResponseEntity<DomainUser> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/users/{name}", DomainUser.class, expected.getUserName());
    softly.assertThat(response.getStatusCode())
        .as("Get user and expect, that status is 200")
        .isEqualTo(HttpStatus.OK);
    DomainUser actual = response.getBody();
    softly.assertThat(actual)
        .as("Get user")
        .isEqualTo(expected);
  }

  /**
   * Gets user avatar.
   */
  @Test
  @Disabled
  void getUserAvatar() {
    DomainUser expected = findFirst();
    ResponseEntity<byte[]> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/users/{name}/avatar", byte[].class, expected.getUserName());
    assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  /**
   * Update user.
   *
   * @param softly the softly
   */
  @Test
  @Disabled
  void updateUser(SoftAssertions softly) {
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
    softly.assertThat(response.getStatusCode())
        .as("Update user and expect, that status is 200")
        .isEqualTo(HttpStatus.OK);
    DomainUser actual = response.getBody();
    softly.assertThat(actual)
        .as("Update user")
        .isNotNull()
        .extracting(DomainUser::getDescription)
        .isEqualTo(expected.getDescription());
  }

  /**
   * Update user password.
   */
  @Test
  @Disabled
  void updateUserPassword() {
    String userName = findFirst().getUserName();
    ResponseEntity<Void> response = restTemplate
        .withBasicAuth(user, pass)
        .exchange("/api/users/{name}/password",
            HttpMethod.PUT,
            new HttpEntity<>(Password.builder().value(UUID.randomUUID().toString()).build()),
            Void.class,
            userName);
    assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.OK);
  }

  /**
   * Update user password and expect forbidden.
   */
  @Test
  @Disabled
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
    assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
  }

  /**
   * Update and remove user avatar.
   *
   * @param softly the softly
   */
  @Test
  @Disabled
  void updateAndRemoveUserAvatar(SoftAssertions softly) {
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
    softly.assertThat(response.getStatusCode())
        .as("Update user avatar and expect, that status is 200")
        .isEqualTo(HttpStatus.OK);

    ResponseEntity<byte[]> avatarResponse = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/users/{name}/avatar", byte[].class, userName);
    byte[] actualBytes = avatarResponse.getBody();
    softly.assertThat(actualBytes)
        .as("Updated user avatar is present")
        .isNotNull();

    restTemplate
        .withBasicAuth(user, pass)
        .delete("/api/users/{name}/avatar", userName);

    avatarResponse = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/users/{name}/avatar", byte[].class, userName);
    softly.assertThat(avatarResponse.getStatusCode())
        .as("User avatar was deleted")
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  /**
   * User exists.
   *
   * @param softly the softly
   */
  @Test
  @Disabled
  void userExists(SoftAssertions softly) {
    DomainUser expected = findFirst();
    ResponseEntity<Boolean> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/users/{name}/exists", Boolean.class, expected.getUserName());
    softly.assertThat(response.getStatusCode())
        .as("Call 'user exists' (1) and expect, that status is 200")
        .isEqualTo(HttpStatus.OK);
    Boolean actual = response.getBody();
    softly.assertThat(actual)
        .as("Call 'user exists' (1) and expect, that is is true")
        .isTrue();

    response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/users/{name}/exists", Boolean.class, UUID.randomUUID().toString());
    softly.assertThat(response.getStatusCode())
        .as("Call 'user exists' (2) and expect, that status is 200")
        .isEqualTo(HttpStatus.OK);
    actual = response.getBody();
    softly.assertThat(actual)
        .as("Call 'user exists' (2) and expect, that is is false")
        .isFalse();
  }

  /**
   * Is user name in use.
   *
   * @param softly the softly
   */
  @Test
  @Disabled
  void isUserNameInUse(SoftAssertions softly) {
    DomainUser expected = findFirst();
    ResponseEntity<Boolean> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/users/{name}/in-use", Boolean.class, expected.getUserName());
    softly.assertThat(response.getStatusCode())
        .as("Call 'is user name in use' (1) and expect, that status is 200")
        .isEqualTo(HttpStatus.OK);
    Boolean actual = response.getBody();
    softly.assertThat(actual)
        .as("Call 'is user name in use' (1) and expect, that is is true")
        .isTrue();

    response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/users/{name}/in-use", Boolean.class, UUID.randomUUID().toString());
    softly.assertThat(response.getStatusCode())
        .as("Call 'is user name in use' (2) and expect, that status is 200")
        .isEqualTo(HttpStatus.OK);
    actual = response.getBody();
    softly.assertThat(actual)
        .as("Call 'is user name in use' (2) and expect, that is is false")
        .isFalse();
  }

  /**
   * Delete user.
   *
   * @param softly the softly
   */
  @Test
  @Disabled
  void deleteUser(SoftAssertions softly) {
    DomainUser expected = findFirst();
    ResponseEntity<Boolean> response = restTemplate
        .withBasicAuth(user, pass)
        .exchange("/api/users/{name}",
            HttpMethod.DELETE,
            null,
            Boolean.class,
            expected.getUserName());
    softly.assertThat(response.getStatusCode())
        .as("Delete user (1) and expect, that status is 200")
        .isEqualTo(HttpStatus.OK);
    Boolean actual = response.getBody();
    softly.assertThat(actual)
        .as("Delete user (1) and expect, that it is true")
        .isTrue();

    response = restTemplate
        .withBasicAuth(user, pass)
        .exchange("/api/users/{name}",
            HttpMethod.DELETE,
            null,
            Boolean.class,
            UUID.randomUUID().toString());
    softly.assertThat(response.getStatusCode())
        .as("Delete user (2) and expect, that status is 200")
        .isEqualTo(HttpStatus.OK);
    actual = response.getBody();
    softly.assertThat(actual)
        .as("Delete user (2) and expect, that it is false")
        .isFalse();
  }

  private List<String> findDomainGroups(final String userName) {
    return List.of(); // TODO
    /*
    try {
      return TestReadWriteUtils.readJsonFromClassPath(
              "demo/groups.json",
              objectMapperBuilder.build(),
              new TypeReference<List<DomainGroup>>() {
              })
          .stream()
          .filter(domainGroup -> domainGroup.getMembers().contains(userName))
          .map(DomainGroup::getName)
          .sorted()
          .collect(Collectors.toList());

    } catch (Exception e) {
      throw new TestFrameworkException(e.getMessage(), e);
    }
    */
  }

  private List<DomainUser> findUsers(Sort sort) {
    return List.of(); // TODO
    /*
    try {
      return TestReadWriteUtils.readJsonFromClassPath(
              "demo/users.json",
              objectMapperBuilder.build(),
              new TypeReference<List<DomainUser>>() {
              })
          .stream()
          .map(domainUser -> domainUser
              .toBuilder()
              .groups(findDomainGroups(domainUser.getUserName()))
              .build())
          .sorted(ComparatorBuilder.newInstance().addAll(SortMapper.fromSort(sort)).build())
          .collect(Collectors.toList());

    } catch (Exception e) {
      throw new TestFrameworkException(e.getMessage(), e);
    }
    */
  }

  private DomainUser findFirst() {
    return findUsers(Sort.by("userName")).get(0);
  }

}