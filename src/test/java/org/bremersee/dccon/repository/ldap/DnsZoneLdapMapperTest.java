package org.bremersee.dccon.repository.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.Month;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DnsZone;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ldaptive.AttributeModification;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;

/**
 * The dns zone ldap mapper test.
 *
 * @author Christian Bremer
 */
public class DnsZoneLdapMapperTest {

  private static DnsZoneLdapMapper mapper;

  /**
   * Init.
   */
  @BeforeClass
  public static void init() {
    DomainControllerProperties properties = new DomainControllerProperties();
    properties.setDnsZoneBaseDn("cn=zones,dc=example,dc=org");
    properties.setDnsZoneRdn("dc");
    mapper = new DnsZoneLdapMapper(properties);
  }

  /**
   * Map distinguished name.
   */
  @Test
  public void mapDn() {
    assertEquals("dc=example.org,cn=zones,dc=example,dc=org",
        mapper.mapDn(DnsZone.builder().name("example.org").build()));
  }

  /**
   * Map.
   */
  @Test
  public void map() {
    LdapEntry ldapEntry = new LdapEntry();
    ldapEntry.setDn("dc=example.org,cn=zones,dc=example,dc=org");
    ldapEntry.addAttribute(new LdapAttribute(
        AbstractLdapMapper.WHEN_CREATED, "20170520150034.000Z"));
    ldapEntry.addAttribute(new LdapAttribute(
        AbstractLdapMapper.WHEN_CHANGED, "20180621160135.000Z"));
    ldapEntry.addAttribute(new LdapAttribute("name", "example.org"));

    DnsZone dnsZone = mapper.map(ldapEntry);
    assertNotNull(dnsZone);
    assertEquals(
        "dc=example.org,cn=zones,dc=example,dc=org",
        dnsZone.getDistinguishedName());
    assertEquals("example.org", dnsZone.getName());

    assertEquals(2017, dnsZone.getCreated().getYear());
    assertEquals(Month.MAY, dnsZone.getCreated().getMonth());
    assertEquals(20, dnsZone.getCreated().getDayOfMonth());
    assertEquals(15, dnsZone.getCreated().getHour());
    assertEquals(0, dnsZone.getCreated().getMinute());
    assertEquals(34, dnsZone.getCreated().getSecond());

    assertEquals(2018, dnsZone.getModified().getYear());
    assertEquals(Month.JUNE, dnsZone.getModified().getMonth());
    assertEquals(21, dnsZone.getModified().getDayOfMonth());
    assertEquals(16, dnsZone.getModified().getHour());
    assertEquals(1, dnsZone.getModified().getMinute());
    assertEquals(35, dnsZone.getModified().getSecond());
  }

  /**
   * Map and compute modifications.
   */
  @Test
  public void mapAndComputeModifications() {
    // A dns zone ldap entry cannot be changed
    AttributeModification[] modifications = mapper.mapAndComputeModifications(
        new DnsZone(), new LdapEntry());
    assertNotNull(modifications);
    assertEquals(0, modifications.length);
  }

}