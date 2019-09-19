package org.bremersee.dccon.repository.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DnsNode;
import org.bremersee.dccon.model.DnsRecord;
import org.bremersee.dccon.model.UnknownFilter;
import org.junit.Test;
import org.ldaptive.AttributeModification;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;

/**
 * The dns node ldap mapper test.
 *
 * @author Christian Bremer
 */
public class DnsNodeLdapMapperTest {

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
   * Test distinguished name.
   */
  @Test
  public void mapDn() {
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
  public void map() {
    DnsNode actual = getMapper(UnknownFilter.ALL).map(null);
    assertNull(actual);

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

    final LdapAttribute recordAttr = new LdapAttribute(true);
    recordAttr.setName("dnsRecord");
    recordAttr.addBinaryValues(Arrays.asList(recordAttrValue0, recordAttrValue1, recordAttrValue2));

    final LdapEntry source = new LdapEntry();
    source.addAttribute(nodeName);

    actual = getMapper(UnknownFilter.NO_UNKNOWN).map(source);
    assertNull(actual);

    actual = getMapper(UnknownFilter.UNKNOWN).map(source);
    assertNull(actual);

    actual = getMapper(UnknownFilter.ALL).map(source);
    assertNotNull(actual);
    assertEquals("proxy", actual.getName());

    source.addAttribute(recordAttr);

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
  public void mapAndComputeModifications() {
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
    final LdapAttribute recordAttr = new LdapAttribute(true);
    recordAttr.setName("dnsRecord");
    recordAttr.addBinaryValues(Arrays.asList(recordAttrValue0, recordAttrValue1, recordAttrValue2));

    final LdapEntry destination = new LdapEntry();
    destination.addAttribute(nodeName);
    destination.addAttribute(recordAttr);

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