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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.iterable;

import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DnsNode;
import org.bremersee.dccon.model.DnsRecord;
import org.bremersee.dccon.model.UnknownFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ldaptive.AttributeModification;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;

/**
 * The dns node ldap mapper test.
 *
 * @author Christian Bremer
 */
@ExtendWith(SoftAssertionsExtension.class)
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
    assertThat(getMapper(UnknownFilter.ALL).getObjectClasses())
        .isEmpty();
  }

  /**
   * Test distinguished name.
   *
   * @param softly the soft assertions
   */
  @Test
  void mapDn(SoftAssertions softly) {
    String dn = getMapper(UnknownFilter.NO_UNKNOWN)
        .mapDn(DnsNode.builder().name("proxy").build());
    softly.assertThat(dn)
        .isEqualTo("dc=proxy,dc=eixe,dc=bremersee,dc=org");

    dn = getMapper(null)
        .mapDn(DnsNode.builder().name("ns0").build());
    softly.assertThat(dn)
        .isEqualTo("dc=ns0,dc=eixe,dc=bremersee,dc=org");
  }

  /**
   * Map ldap entry.
   *
   * @param softly the soft assertions
   */
  @Test
  void map(SoftAssertions softly) {
    DnsNode actual = getMapper(UnknownFilter.ALL).map(null);
    softly.assertThat(actual)
        .isNull();

    DnsNode dnsNode = DnsNode.builder().build();
    getMapper(UnknownFilter.ALL).map(null, dnsNode);
    softly.assertThat(dnsNode)
        .isEqualTo(DnsNode.builder().build());

    LdapAttribute nodeName = new LdapAttribute("name", "proxy");

    LdapEntry source = new LdapEntry();
    source.addAttributes(nodeName);

    actual = getMapper(UnknownFilter.NO_UNKNOWN).map(source);
    softly.assertThat(actual)
        .isNull();

    actual = getMapper(UnknownFilter.ALL).map(source);
    softly.assertThat(actual)
        .isNotNull()
        .extracting(DnsNode::getName)
        .isEqualTo("proxy");

    // A : 192.168.1.41
    byte[] recordAttrValue0 = Base64.getDecoder().decode(
        "BAABAAXwAAAmWwAAAAADhAAAAAD46jcAwKgBKQ==");
    // UNKNOWN
    byte[] recordAttrValue1 = Base64.getDecoder().decode(
        "CAAAAAUAAACSWgAAAAAAAAAAAAAAAAAA0iISorCx1AE=");
    // CNAME : lb.eixe.bremersee.org
    byte[] recordAttrValue2 = Base64.getDecoder().decode(
        "GQAFAAXwAAB5HwAAAAADhAAAAADy4TcAFwQCbGIEZWl4ZQlicmVtZXJzZWUDb3JnAA==");
    LdapAttribute recordAttr = new LdapAttribute();
    recordAttr.setName("dnsRecord");
    recordAttr.setBinary(true);
    recordAttr.addBinaryValues(Arrays.asList(recordAttrValue0, recordAttrValue1, recordAttrValue2));
    source.addAttributes(recordAttr);

    actual = getMapper(UnknownFilter.NO_UNKNOWN).map(source);
    softly.assertThat(actual)
        .extracting(DnsNode::getName)
        .isEqualTo("proxy");
    softly.assertThat(actual)
        .extracting(DnsNode::getRecords, iterable(DnsRecord.class))
        .contains(
            DnsRecord.builder()
                .recordType("A")
                .recordValue("192.168.1.41")
                .build(),
            DnsRecord.builder()
                .recordType("CNAME")
                .recordValue("lb.eixe.bremersee.org")
                .build());
    softly.assertThat(actual)
        .extracting(DnsNode::getRecords, iterable(DnsRecord.class))
        .map(DnsRecord::getRecordType)
        .map(String::toLowerCase)
        .doesNotContain("unknown");

    actual = getMapper(UnknownFilter.UNKNOWN).map(source);
    softly.assertThat(actual)
        .isNotNull()
        .extracting(DnsNode::getName)
        .isEqualTo("proxy");
    softly.assertThat(actual)
        .extracting(DnsNode::getRecords, iterable(DnsRecord.class))
        .doesNotContain(
            DnsRecord.builder()
                .recordType("A")
                .recordValue("192.168.1.41")
                .build(),
            DnsRecord.builder()
                .recordType("CNAME")
                .recordValue("lb.eixe.bremersee.org")
                .build());
    softly.assertThat(actual)
        .extracting(DnsNode::getRecords, iterable(DnsRecord.class))
        .map(DnsRecord::getRecordType)
        .map(String::toLowerCase)
        .contains("unknown");

    actual = getMapper(UnknownFilter.ALL).map(source);
    softly.assertThat(actual)
        .extracting(DnsNode::getName)
        .isEqualTo("proxy");
    softly.assertThat(actual)
        .extracting(DnsNode::getRecords, iterable(DnsRecord.class))
        .contains(
            DnsRecord.builder()
                .recordType("A")
                .recordValue("192.168.1.41")
                .build(),
            DnsRecord.builder()
                .recordType("CNAME")
                .recordValue("lb.eixe.bremersee.org")
                .build());
    softly.assertThat(actual)
        .extracting(DnsNode::getRecords, iterable(DnsRecord.class))
        .map(DnsRecord::getRecordType)
        .map(String::toLowerCase)
        .contains("unknown");
  }

  /**
   * Map and compute modifications.
   *
   * @param softly the soft assertions
   */
  @Test
  void mapAndComputeModifications(SoftAssertions softly) {
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
    softly.assertThat(destination.getAttribute("dnsRecord").getBinaryValues())
        .contains(recordAttrValue0, recordAttrValue1, recordAttrValue2);

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

    AttributeModification[] modifications = mapper
        .mapAndComputeModifications(source, destination);
    softly.assertThat(modifications)
        .hasSizeGreaterThan(0);
    softly.assertThat(destination)
        .extracting(dest -> dest.getAttribute("name"))
        .extracting(LdapAttribute::getStringValue)
        .isEqualTo("proxy");
    // record 0 and 2 must still be present
    // record 1 must be removed
    softly.assertThat(destination.getAttribute("dnsRecord").getBinaryValues())
        .containsExactlyInAnyOrder(recordAttrValue0, recordAttrValue2)
        .doesNotContain(recordAttrValue1);
  }
}