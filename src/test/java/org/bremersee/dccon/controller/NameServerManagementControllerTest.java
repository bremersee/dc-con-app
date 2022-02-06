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

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DhcpLeasePage;
import org.bremersee.dccon.model.DnsNode;
import org.bremersee.dccon.model.DnsNodePage;
import org.bremersee.dccon.model.DnsRecord;
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.model.DnsZonePage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * The name server management controller test.
 *
 * @author Christian Bremer
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"in-memory"})
@ExtendWith(SoftAssertionsExtension.class)
class NameServerManagementControllerTest {

  private static final String user = "admin";

  private static final String pass = "admin";

  /**
   * The Properties.
   */
  @Autowired
  DomainControllerProperties properties;

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
    restTemplate
        .withBasicAuth(user, pass)
        .postForEntity("/api/reset", null, Void.class);
  }

  /**
   * Query.
   *
   * @param softly the softly
   */
  @Test
  void query(SoftAssertions softly) {
    DnsNode expected = DnsNode.builder()
        .name("newtestnode")
        .records(Collections.singleton(DnsRecord.builder()
            .recordType("A")
            .recordValue("192.168.1.254").build()))
        .build();
    ResponseEntity<DnsNode> addResponse = restTemplate
        .withBasicAuth(user, pass)
        .postForEntity("/api/dns/zones/{zoneName}",
            expected,
            DnsNode.class,
            properties.getDefaultZone());
    softly.assertThat(addResponse.getStatusCode())
        .isEqualTo(HttpStatus.OK);

    DnsNode actual = addResponse.getBody();
    softly.assertThat(actual)
        .isNotNull()
        .extracting(DnsNode::getName)
        .isEqualTo(expected.getName());

    ResponseEntity<DnsNodePage> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/dns?q={query}",
            DnsNodePage.class,
            "newtestnode");
    softly.assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.OK);

    DnsNodePage result = response.getBody();
    softly.assertThat(result)
        .isNotNull()
        .extracting(DnsNodePage::getContent)
        .extracting(List::size, InstanceOfAssertFactories.INTEGER)
        .isGreaterThan(0);
  }

  /**
   * Gets dhcp leases.
   *
   * @param softly the softly
   */
  @Test
  void getDhcpLeases(SoftAssertions softly) {
    ResponseEntity<DhcpLeasePage> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/dns/dhcp-leases", DhcpLeasePage.class);
    softly.assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.OK);

    DhcpLeasePage actual = response.getBody();
    softly.assertThat(actual)
        .isNotNull()
        .extracting(DhcpLeasePage::getContent)
        .extracting(List::size, InstanceOfAssertFactories.INTEGER)
        .isGreaterThan(0);
  }

  /**
   * Gets dns zones.
   *
   * @param softly the softly
   */
  @Test
  void getDnsZones(SoftAssertions softly) {
    ResponseEntity<DnsZonePage> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/dns/zones", DnsZonePage.class);
    softly.assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.OK);

    DnsZonePage actual = response.getBody();
    softly.assertThat(actual)
        .isNotNull()
        .extracting(DnsZonePage::getContent)
        .extracting(List::size, InstanceOfAssertFactories.INTEGER)
        .isGreaterThan(0);
  }

  /**
   * Add dns zone and delete.
   *
   * @param softly the softly
   */
  @Test
  void addDnsZoneAndDelete(SoftAssertions softly) {
    DnsZone expected = DnsZone.builder().name("newtestzone").build();
    ResponseEntity<DnsZone> addResponse = restTemplate
        .withBasicAuth(user, pass)
        .postForEntity("/api/dns/zones", expected, DnsZone.class);
    softly.assertThat(addResponse.getStatusCode())
        .isEqualTo(HttpStatus.OK);

    DnsZone actual = addResponse.getBody();
    softly.assertThat(actual)
        .isNotNull()
        .extracting(DnsZone::getName)
        .isEqualTo(expected.getName());

    ResponseEntity<Boolean> response = restTemplate
        .withBasicAuth(user, pass)
        .exchange("/api/dns/zones/{name}",
            HttpMethod.DELETE,
            null,
            Boolean.class,
            expected.getName());
    softly.assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.OK);

    Boolean deleted = response.getBody();
    softly.assertThat(deleted)
        .isTrue();

    response = restTemplate
        .withBasicAuth(user, pass)
        .exchange("/api/dns/zones/{name}",
            HttpMethod.DELETE,
            null,
            Boolean.class,
            UUID.randomUUID().toString());
    softly.assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.OK);

    deleted = response.getBody();
    softly.assertThat(deleted)
        .isFalse();
  }

  /**
   * Gets dns nodes.
   *
   * @param softly the softly
   */
  @Test
  void getDnsNodes(SoftAssertions softly) {
    ResponseEntity<DnsNodePage> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/dns/zones/{zoneName}",
            DnsNodePage.class,
            properties.getDefaultZone());
    softly.assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.OK);

    DnsNodePage actual = response.getBody();
    softly.assertThat(actual)
        .isNotNull()
        .extracting(DnsNodePage::getContent)
        .extracting(List::size, InstanceOfAssertFactories.INTEGER)
        .isGreaterThan(0);
  }

  /**
   * Save dns node and delete.
   *
   * @param softly the softly
   */
  @Test
  void saveDnsNodeAndDelete(SoftAssertions softly) {
    DnsNode expected = DnsNode.builder()
        .name("newtestnode")
        .records(Collections.singleton(DnsRecord.builder()
            .recordType("A")
            .recordValue("192.168.1.254").build()))
        .build();
    ResponseEntity<DnsNode> addResponse = restTemplate
        .withBasicAuth(user, pass)
        .postForEntity("/api/dns/zones/{zoneName}",
            expected,
            DnsNode.class,
            properties.getDefaultZone());
    softly.assertThat(addResponse.getStatusCode())
        .isEqualTo(HttpStatus.OK);

    DnsNode actual = addResponse.getBody();
    softly.assertThat(actual)
        .isNotNull()
        .extracting(DnsNode::getName)
        .isEqualTo(expected.getName());

    ResponseEntity<Boolean> response = restTemplate
        .withBasicAuth(user, pass)
        .exchange("/api/dns/zones/{zoneName}/{nodeName}",
            HttpMethod.DELETE,
            null,
            Boolean.class,
            properties.getDefaultZone(),
            expected.getName());
    softly.assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.OK);

    Boolean deleted = response.getBody();
    softly.assertThat(deleted)
        .isTrue();

    response = restTemplate
        .withBasicAuth(user, pass)
        .exchange("/api/dns/zones/{zoneName}/{nodeName}",
            HttpMethod.DELETE,
            null,
            Boolean.class,
            properties.getDefaultZone(),
            UUID.randomUUID().toString());
    softly.assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.OK);

    deleted = response.getBody();
    softly.assertThat(deleted)
        .isFalse();
  }

  /**
   * Save and get dns node.
   *
   * @param softly the softly
   */
  @Test
  void saveAndGetDnsNode(SoftAssertions softly) {
    DnsNode expected = DnsNode.builder()
        .name("newtestnode")
        .records(Collections.singleton(DnsRecord.builder()
            .recordType("A")
            .recordValue("192.168.1.254").build()))
        .build();
    ResponseEntity<DnsNode> response = restTemplate
        .withBasicAuth(user, pass)
        .postForEntity("/api/dns/zones/{zoneName}",
            expected,
            DnsNode.class,
            properties.getDefaultZone());
    softly.assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.OK);

    DnsNode actual = response.getBody();
    softly.assertThat(actual)
        .isNotNull()
        .extracting(DnsNode::getName)
        .isEqualTo(expected.getName());

    response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/dns/zones/{zoneName}/{nodeName}",
            DnsNode.class,
            properties.getDefaultZone(),
            expected.getName());
    softly.assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.OK);

    actual = response.getBody();
    softly.assertThat(actual)
        .isNotNull()
        .extracting(DnsNode::getName)
        .isEqualTo(expected.getName());
  }

  /**
   * Delete all dns nodes.
   */
  @Test
  void deleteAllDnsNodes() {
    ResponseEntity<String> response = restTemplate
        .withBasicAuth(user, pass)
        .exchange("/api/dns/zones/{zoneName}/nodes/all",
            HttpMethod.DELETE,
            null,
            String.class,
            properties.getDefaultZone());
    assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.OK);
  }
}