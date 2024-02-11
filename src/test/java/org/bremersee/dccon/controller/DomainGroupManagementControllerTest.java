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

import java.util.List;
import java.util.UUID;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.bremersee.comparator.ComparatorBuilder;
import org.bremersee.dccon.model.CommonAttributes;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.model.DomainGroupPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.ActiveProfiles;

/**
 * The domain group management controller test.
 *
 * @author Christian Bremer
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"in-memory"})
@ExtendWith(SoftAssertionsExtension.class)
class DomainGroupManagementControllerTest {

  private static final String user = "admin";

  private static final String pass = "admin";

  /**
   * The rest template.
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
   * Gets groups.
   *
   * @param softly the softly
   */
  @Test
  @Disabled
  void getGroups(SoftAssertions softly) {
    ResponseEntity<DomainGroupPage> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/groups", DomainGroupPage.class);
    softly.assertThat(response.getStatusCode())
        .as("Get groups without pageable and expect, that status is 200")
        .isEqualTo(HttpStatus.OK);
    DomainGroupPage actual = response.getBody();
    List<DomainGroup> expectedContent = findGroups(Sort
        .by(Order.by(DomainGroup.NAME).ignoreCase()));
    softly.assertThat(actual)
        .isNotNull()
        .extracting(DomainGroupPage::getContent, InstanceOfAssertFactories.list(DomainGroup.class))
        .as("Get groups without pageable and expect content of groups.json")
        .containsExactlyInAnyOrderElementsOf(expectedContent);

    response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/groups?page=0&size=3&sort=distinguishedName,desc",
            DomainGroupPage.class);
    softly.assertThat(response.getStatusCode())
        .as("Get groups with pageable and expect status is 200")
        .isEqualTo(HttpStatus.OK);
    actual = response.getBody();
    expectedContent.sort(ComparatorBuilder.newInstance()
        .add(CommonAttributes.DISTINGUISHED_NAME, false, true, false)
        .build());
    softly.assertThat(actual)
        .isNotNull()
        .extracting(DomainGroupPage::getContent, InstanceOfAssertFactories.list(DomainGroup.class))
        .as("Get groups with pageable and expect three entries")
        .containsExactlyElementsOf(expectedContent.subList(0, 3));
  }

  /**
   * Add group.
   *
   * @param softly the softly
   */
  @Test
  @Disabled
  void addGroup(SoftAssertions softly) {
    DomainGroup source = DomainGroup.builder()
        .name(UUID.randomUUID().toString())
        .description("A new test group.")
        .build();
    ResponseEntity<DomainGroup> response = restTemplate
        .withBasicAuth(user, pass)
        .postForEntity("/api/groups", source, DomainGroup.class);
    softly.assertThat(response.getStatusCode())
        .as("Add group and expect, that status is 200")
        .isEqualTo(HttpStatus.OK);
    DomainGroup actual = response.getBody();
    softly.assertThat(actual)
        .as("Add group and assert, that response is not null")
        .isNotNull()
        .extracting(DomainGroup::getName)
        .as("Add group and assert, that names are equal")
        .isEqualTo(source.getName());
    softly.assertThat(actual)
        .isNotNull()
        .as("Add group and assert, that response is not null")
        .extracting(DomainGroup::getDescription)
        .as("Add group and assert, that descriptions are equal")
        .isEqualTo(source.getDescription());
  }

  /**
   * Gets group.
   *
   * @param softly the softly
   */
  @Test
  @Disabled
  void getGroup(SoftAssertions softly) {
    DomainGroup expected = findFirst();
    ResponseEntity<DomainGroup> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/groups/{name}", DomainGroup.class, expected.getName());
    softly.assertThat(response.getStatusCode())
        .as("Get group and expect, that status is 200")
        .isEqualTo(HttpStatus.OK);
    DomainGroup actual = response.getBody();
    softly.assertThat(actual)
        .as("Get group")
        .isEqualTo(expected);
  }

  /**
   * Update group.
   *
   * @param softly the softly
   */
  @Test
  @Disabled
  void updateGroup(SoftAssertions softly) {
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
    softly.assertThat(response.getStatusCode())
        .as("Update group and expect, that status is 200")
        .isEqualTo(HttpStatus.OK);
    DomainGroup actual = response.getBody();
    softly.assertThat(actual)
        .isNotNull()
        .as("Update group and assert, that response is not null")
        .extracting(DomainGroup::getDescription)
        .as("Update group and assert, that descriptions are equal")
        .isEqualTo(expected.getDescription());
  }

  /**
   * Group exists.
   *
   * @param softly the softly
   */
  @Test
  @Disabled
  void groupExists(SoftAssertions softly) {
    DomainGroup expected = findFirst();
    ResponseEntity<Boolean> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/groups/{name}/exists", Boolean.class, expected.getName());
    softly.assertThat(response.getStatusCode())
        .as("Call 'group exists' (1) and expect, that status is 200")
        .isEqualTo(HttpStatus.OK);
    Boolean actual = response.getBody();
    softly.assertThat(actual)
        .as("Call 'group exists' (1) and expect, that is is true")
        .isTrue();

    response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/groups/{name}/exists", Boolean.class, UUID.randomUUID().toString());
    softly.assertThat(response.getStatusCode())
        .as("Call 'group exists' (2) and expect, that status is 200")
        .isEqualTo(HttpStatus.OK);
    actual = response.getBody();
    softly.assertThat(actual)
        .as("Call 'group exists' (2) and expect, that is is false")
        .isFalse();
  }

  /**
   * Is group name in use.
   *
   * @param softly the softly
   */
  @Test
  @Disabled
  void isGroupNameInUse(SoftAssertions softly) {
    DomainGroup expected = findFirst();
    ResponseEntity<Boolean> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/groups/{name}/in-use", Boolean.class, expected.getName());
    softly.assertThat(response.getStatusCode())
        .as("Call 'is group name in use' (1) and expect, that status is 200")
        .isEqualTo(HttpStatus.OK);
    Boolean actual = response.getBody();
    softly.assertThat(actual)
        .as("Call 'is group name in use' (1) and expect, that is is true")
        .isTrue();

    response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/groups/{name}/in-use", Boolean.class, UUID.randomUUID().toString());
    softly.assertThat(response.getStatusCode())
        .as("Call 'is group name in use' (2) and expect, that status is 200")
        .isEqualTo(HttpStatus.OK);
    actual = response.getBody();
    softly.assertThat(actual)
        .as("Call 'is group name in use' (2) and expect, that is is false")
        .isFalse();
  }

  /**
   * Delete group.
   *
   * @param softly the softly
   */
  @Test
  @Disabled
  void deleteGroup(SoftAssertions softly) {
    DomainGroup expected = findFirst();
    ResponseEntity<Boolean> response = restTemplate
        .withBasicAuth(user, pass)
        .exchange("/api/groups/{name}",
            HttpMethod.DELETE,
            null,
            Boolean.class,
            expected.getName());
    softly.assertThat(response.getStatusCode())
        .as("Delete group (1) and expect, that status is 200")
        .isEqualTo(HttpStatus.OK);
    Boolean actual = response.getBody();
    softly.assertThat(actual)
        .as("Delete group (1) and expect, that it is true")
        .isTrue();

    response = restTemplate
        .withBasicAuth(user, pass)
        .exchange("/api/groups/{name}",
            HttpMethod.DELETE,
            null,
            Boolean.class,
            UUID.randomUUID().toString());
    softly.assertThat(response.getStatusCode())
        .as("Delete group (2) and expect, that status is 200")
        .isEqualTo(HttpStatus.OK);
    actual = response.getBody();
    softly.assertThat(actual)
        .as("Delete group (2) and expect, that it is false")
        .isFalse();
  }

  private List<DomainGroup> findGroups(Sort sort) {
    return List.of(); // TODO
    /*
    try {
      List<DomainGroup> list = TestReadWriteUtils.readJsonFromClassPath(
          "demo/groups.json",
          objectMapperBuilder.build(),
          new TypeReference<>() {
          });
      list.sort(ComparatorBuilder.newInstance().addAll(SortMapper.fromSort(sort)).build());
      return list;

    } catch (Exception e) {
      throw new TestFrameworkException(e.getMessage(), e);
    }

     */
  }

  private DomainGroup findFirst() {
    return null; // TODO
    /*
    try {
      List<DomainGroup> list = TestReadWriteUtils.readJsonFromClassPath(
          "demo/groups.json",
          objectMapperBuilder.build(),
          new TypeReference<>() {
          });
      return list.get(0);

    } catch (Exception e) {
      throw new TestFrameworkException(e.getMessage(), e);
    }
    */
  }
}