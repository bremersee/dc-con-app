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

package org.bremersee.dccon.repository.ldap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DnsNode;
import org.bremersee.dccon.model.DnsRecord;
import org.bremersee.dccon.model.UnknownFilter;
import org.junit.jupiter.api.Test;
import org.ldaptive.AttributeModification;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;

/**
 * The dns node ldap mapper test.
 *
 * @author Christian Bremer
 */
class DnsNodeLdapMapperTest {

  private DnsNodeLdapMapper getMapper(UnknownFilter unknownFilter) {
    final DomainControllerProperties properties = new DomainControllerProperties();
    properties.setDnsNodeBaseDn("dc=eixe,dc=bremersee,dc=org");
    properties.setDnsNodeRdn("dc");
    return new DnsNodeLdapMapper(
        properties,
        "eixe.bremersee.org",
        unknownFilter);
  }

  /**
   * Gets object classes.
   */
  @Test
  void getObjectClasses() {
    assertArrayEquals(new String[0], getMapper(UnknownFilter.ALL).getObjectClasses());
  }

  /**
   * Test distinguished name.
   */
  @Test
  void mapDn() {
    String dn = getMapper(UnknownFilter.NO_UNKNOWN)
        .mapDn(DnsNode.builder().name("proxy").build());
    assertEquals("dc=proxy,dc=eixe,dc=bremersee,dc=org", dn);

    dn = getMapper(null)
        .mapDn(DnsNode.builder().name("proxy").build());
    assertEquals("dc=proxy,dc=eixe,dc=bremersee,dc=org", dn);
  }

  /**
   * Map ldap entry.
   */
  @Test
  void map() {
    DnsNode actual = getMapper(UnknownFilter.ALL).map(null);
    assertNull(actual);

    DnsNode dnsNode = DnsNode.builder().build();
    getMapper(UnknownFilter.ALL).map(null, dnsNode);
    assertEquals(DnsNode.builder().build(), dnsNode);

    final LdapAttribute nodeName = new LdapAttribute("name", "proxy");
    // A : 192.168.1.41
    final byte[] recordAttrValue0 = Base64.getDecoder().decode(
        "BAABAAXwAAAmWwAAAAADhAAAAAD46jcAwKgBKQ==");
    // UNKNOWN
    final byte[] recordAttrValue1 = Base64.getDecoder().decode(
        "CAAAAAUAAACSWgAAAAAAAAAAAAAAAAAA0iISorCx1AE=");
    // CNAME : lb.eixe.bremersee.org
    final byte[] recordAttrValue2 = Base64.getDecoder().decode(
        "GQAFAAXwAAB5HwAAAAADhAAAAADy4TcAFwQCbGIEZWl4ZQlicmVtZXJzZWUDb3JnAA==");

    final LdapAttribute recordAttr = new LdapAttribute();
    recordAttr.setName("dnsRecord");
    recordAttr.setBinary(true);
    recordAttr.addBinaryValues(Arrays.asList(recordAttrValue0, recordAttrValue1, recordAttrValue2));

    final LdapEntry source = new LdapEntry();
    source.addAttributes(nodeName);

    actual = getMapper(UnknownFilter.NO_UNKNOWN).map(source);
    assertNull(actual);

    actual = getMapper(UnknownFilter.UNKNOWN).map(source);
    assertNull(actual);

    actual = getMapper(UnknownFilter.ALL).map(source);
    assertNotNull(actual);
    assertEquals("proxy", actual.getName());

    source.addAttributes(recordAttr);

    actual = getMapper(UnknownFilter.NO_UNKNOWN).map(source);
    assertNotNull(actual);
    assertEquals("proxy", actual.getName());
    assertTrue(actual.getRecords().contains(DnsRecord.builder()
        .recordType("A")
        .recordValue("192.168.1.41")
        .build()));
    assertTrue(actual.getRecords().contains(DnsRecord.builder()
        .recordType("CNAME")
        .recordValue("lb.eixe.bremersee.org")
        .build()));
    assertFalse(actual.getRecords().stream()
        .anyMatch(record -> record.getRecordType().equalsIgnoreCase("unknown")));

    actual = getMapper(UnknownFilter.UNKNOWN).map(source);
    assertNotNull(actual);
    assertEquals("proxy", actual.getName());
    assertFalse(actual.getRecords().contains(DnsRecord.builder()
        .recordType("A")
        .recordValue("192.168.1.41")
        .build()));
    assertFalse(actual.getRecords().contains(DnsRecord.builder()
        .recordType("CNAME")
        .recordValue("lb.eixe.bremersee.org")
        .build()));
    assertTrue(actual.getRecords().stream()
        .anyMatch(record -> record.getRecordType().equalsIgnoreCase("unknown")));

    actual = getMapper(UnknownFilter.ALL).map(source);
    assertNotNull(actual);
    assertEquals("proxy", actual.getName());
    assertTrue(actual.getRecords().contains(DnsRecord.builder()
        .recordType("A")
        .recordValue("192.168.1.41")
        .build()));
    assertTrue(actual.getRecords().contains(DnsRecord.builder()
        .recordType("CNAME")
        .recordValue("lb.eixe.bremersee.org")
        .build()));
    assertTrue(actual.getRecords().stream()
        .anyMatch(record -> record.getRecordType().equalsIgnoreCase("unknown")));
  }

  /**
   * Map and compute modifications.
   */
  @Test
  void mapAndComputeModifications() {
    final DnsNodeLdapMapper mapper = getMapper(UnknownFilter.ALL);

    final LdapAttribute nodeName = new LdapAttribute("name", "proxy");
    // A : 192.168.1.41
    final byte[] recordAttrValue0 = Base64.getDecoder().decode(
        "BAABAAXwAAAmWwAAAAADhAAAAAD46jcAwKgBKQ==");
    // UNKNOWN
    final byte[] recordAttrValue1 = Base64.getDecoder().decode(
        "CAAAAAUAAACSWgAAAAAAAAAAAAAAAAAA0iISorCx1AE=");
    // CNAME : lb.eixe.bremersee.org
    final byte[] recordAttrValue2 = Base64.getDecoder().decode(
        "GQAFAAXwAAB5HwAAAAADhAAAAADy4TcAFwQCbGIEZWl4ZQlicmVtZXJzZWUDb3JnAA==");
    final LdapAttribute recordAttr = new LdapAttribute();
    recordAttr.setName("dnsRecord");
    recordAttr.setBinary(true);
    recordAttr.addBinaryValues(Arrays.asList(recordAttrValue0, recordAttrValue1, recordAttrValue2));

    final LdapEntry destination = new LdapEntry();
    destination.addAttributes(nodeName, recordAttr);

    assertTrue(destination.getAttribute("dnsRecord").getBinaryValues()
        .contains(recordAttrValue0));
    assertTrue(destination.getAttribute("dnsRecord").getBinaryValues()
        .contains(recordAttrValue1));
    assertTrue(destination.getAttribute("dnsRecord").getBinaryValues()
        .contains(recordAttrValue2));

    final DnsRecord record0 = DnsRecord.builder()
        .recordType("A")
        .recordValue("192.168.1.41")
        .build();
    final DnsRecord record1 = DnsRecord.builder()
        .recordType("CNAME")
        .recordValue("lb.eixe.bremersee.org")
        .build();
    final LinkedHashSet<DnsRecord> records = new LinkedHashSet<>();
    records.add(record0);
    records.add(record1);
    final DnsNode source = DnsNode.builder()
        .name("proxy")
        .records(records)
        .build();

    AttributeModification[] modifications = mapper.mapAndComputeModifications(source, destination);
    assertNotNull(modifications);
    assertEquals("proxy", destination.getAttribute("name").getStringValue());
    assertTrue(modifications.length > 0);
    assertEquals(2, destination.getAttribute("dnsRecord").getBinaryValues().size());

    // record 0 and 2 must still be present
    assertTrue(destination.getAttribute("dnsRecord").getBinaryValues()
        .contains(recordAttrValue0));
    assertTrue(destination.getAttribute("dnsRecord").getBinaryValues()
        .contains(recordAttrValue2));

    // record 1 must be removed
    assertFalse(destination.getAttribute("dnsRecord").getBinaryValues()
        .contains(recordAttrValue1));
  }
}