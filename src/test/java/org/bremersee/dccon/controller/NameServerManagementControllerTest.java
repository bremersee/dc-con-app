package org.bremersee.dccon.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.UUID;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DhcpLease;
import org.bremersee.dccon.model.DnsNode;
import org.bremersee.dccon.model.DnsRecord;
import org.bremersee.dccon.model.DnsZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"in-memory"})
class NameServerManagementControllerTest {

  private static final String user = "admin";

  private static final String pass = "admin";

  @Autowired
  DomainControllerProperties properties;

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
  void query() {
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
    assertEquals(HttpStatus.OK, addResponse.getStatusCode());
    DnsNode actual = addResponse.getBody();
    assertNotNull(actual);
    assertEquals(expected.getName(), actual.getName());

    ResponseEntity<DnsNode[]> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/dns?q={query}",
            DnsNode[].class,
            "newtestnode");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    DnsNode[] result = response.getBody();
    assertNotNull(result);
    assertTrue(result.length > 0);
  }

  @Test
  void getDhcpLeases() {
    ResponseEntity<DhcpLease[]> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/dns/dhcp-leases", DhcpLease[].class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    DhcpLease[] actual = response.getBody();
    assertNotNull(actual);
    assertTrue(actual.length > 0);
  }

  @Test
  void getDnsZones() {
    ResponseEntity<DnsZone[]> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/dns/zones", DnsZone[].class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    DnsZone[] actual = response.getBody();
    assertNotNull(actual);
    assertTrue(actual.length > 0);
  }

  @Test
  void addDnsZoneAndDelete() {
    DnsZone expected = DnsZone.builder().name("newtestzone").build();
    ResponseEntity<DnsZone> addResponse = restTemplate
        .withBasicAuth(user, pass)
        .postForEntity("/api/dns/zones", expected, DnsZone.class);
    assertEquals(HttpStatus.OK, addResponse.getStatusCode());
    DnsZone actual = addResponse.getBody();
    assertNotNull(actual);
    assertEquals(expected.getName(), actual.getName());

    ResponseEntity<Boolean> response = restTemplate
        .withBasicAuth(user, pass)
        .exchange("/api/dns/zones/{name}",
            HttpMethod.DELETE,
            null,
            Boolean.class,
            expected.getName());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    Boolean deleted = response.getBody();
    assertNotNull(deleted);
    assertTrue(deleted);

    response = restTemplate
        .withBasicAuth(user, pass)
        .exchange("/api/dns/zones/{name}",
            HttpMethod.DELETE,
            null,
            Boolean.class,
            UUID.randomUUID().toString());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    deleted = response.getBody();
    assertNotNull(deleted);
    assertFalse(deleted);
  }

  @Test
  void getDnsNodes() {
    ResponseEntity<DnsNode[]> response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/dns/zones/{zoneName}",
            DnsNode[].class,
            properties.getDefaultZone());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    DnsNode[] actual = response.getBody();
    assertNotNull(actual);
    assertTrue(actual.length > 0);
  }

  @Test
  void saveDnsNodeAndDelete() {
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
    assertEquals(HttpStatus.OK, addResponse.getStatusCode());
    DnsNode actual = addResponse.getBody();
    assertNotNull(actual);
    assertEquals(expected.getName(), actual.getName());

    ResponseEntity<Boolean> response = restTemplate
        .withBasicAuth(user, pass)
        .exchange("/api/dns/zones/{zoneName}/{nodeName}",
            HttpMethod.DELETE,
            null,
            Boolean.class,
            properties.getDefaultZone(),
            expected.getName());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    Boolean deleted = response.getBody();
    assertNotNull(deleted);
    assertTrue(deleted);

    response = restTemplate
        .withBasicAuth(user, pass)
        .exchange("/api/dns/zones/{zoneName}/{nodeName}",
            HttpMethod.DELETE,
            null,
            Boolean.class,
            properties.getDefaultZone(),
            UUID.randomUUID().toString());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    deleted = response.getBody();
    assertNotNull(deleted);
    assertFalse(deleted);
  }

  @Test
  void saveAndGetDnsNode() {
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
    assertEquals(HttpStatus.OK, response.getStatusCode());
    DnsNode actual = response.getBody();
    assertNotNull(actual);
    assertEquals(expected.getName(), actual.getName());

    response = restTemplate
        .withBasicAuth(user, pass)
        .getForEntity("/api/dns/zones/{zoneName}/{nodeName}",
            DnsNode.class,
            properties.getDefaultZone(),
            expected.getName());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    actual = response.getBody();
    assertNotNull(actual);
    assertEquals(expected.getName(), actual.getName());
  }

  @Test
  void deleteAllDnsNodes() {
    ResponseEntity<String> response = restTemplate
        .withBasicAuth(user, pass)
        .exchange("/api/dns/zones/{zoneName}/nodes/all",
            HttpMethod.DELETE,
            null,
            String.class,
            properties.getDefaultZone());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}