package org.bremersee.dccon.repository.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Month;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainGroup;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ldaptive.AttributeModification;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;

/**
 * The domain group ldap mapper test.
 *
 * @author Christian Bremer
 */
public class DomainGroupLdapMapperTest {

  private static DomainGroupLdapMapper mapper;

  /**
   * Init.
   */
  @BeforeClass
  public static void init() {
    DomainControllerProperties properties = new DomainControllerProperties();
    properties.setGroupRdn("cn");
    properties.setGroupBaseDn("cn=Users,dc=example,dc=org");
    properties.setUserRdn("cn");
    properties.setUserBaseDn("cn=Users,dc=example,dc=org");
    mapper = new DomainGroupLdapMapper(properties);
  }

  /**
   * Map distinguished name.
   */
  @Test
  public void mapDn() {
    DomainGroup domainGroup = new DomainGroup();
    domainGroup.setName("somename");
    String dn = mapper.mapDn(domainGroup);
    assertNotNull(dn);
    assertEquals("cn=somename,cn=Users,dc=example,dc=org", dn);
  }

  /**
   * Map ldap entry.
   */
  @Test
  public void map() {
    LdapEntry source = new LdapEntry();
    source.setDn("cn=somename,cn=Users,dc=example,dc=org");
    source.addAttribute(new LdapAttribute(
        AbstractLdapMapper.WHEN_CREATED, "20170520150034.000Z"));
    source.addAttribute(new LdapAttribute(
        AbstractLdapMapper.WHEN_CHANGED, "20180621160135.000Z"));
    source.addAttribute(new LdapAttribute("name", "somename"));
    source.addAttribute(new LdapAttribute("member",
        "cn=member1,cn=Users,dc=example,dc=org",
        "cn=member2,cn=Users,dc=example,dc=org"));

    DomainGroup destination = mapper.map(source);
    assertNotNull(destination);
    assertEquals("cn=somename,cn=Users,dc=example,dc=org", destination.getDistinguishedName());
    assertEquals(destination.getCreated(), destination.getCreated());

    assertEquals(2017, destination.getCreated().getYear());
    assertEquals(Month.MAY, destination.getCreated().getMonth());
    assertEquals(20, destination.getCreated().getDayOfMonth());
    assertEquals(15, destination.getCreated().getHour());
    assertEquals(0, destination.getCreated().getMinute());
    assertEquals(34, destination.getCreated().getSecond());

    assertEquals(2018, destination.getModified().getYear());
    assertEquals(Month.JUNE, destination.getModified().getMonth());
    assertEquals(21, destination.getModified().getDayOfMonth());
    assertEquals(16, destination.getModified().getHour());
    assertEquals(1, destination.getModified().getMinute());
    assertEquals(35, destination.getModified().getSecond());

    assertEquals("somename", destination.getName());
    assertTrue(destination.getMembers().contains("member1"));
    assertTrue(destination.getMembers().contains("member2"));
    assertFalse(destination.getMembers().contains("member3"));
  }

  /**
   * Map and compute modifications.
   */
  @Test
  public void mapAndComputeModifications() {
    DomainGroup source = new DomainGroup();
    source.setName("somename");
    source.getMembers().add("member1");
    source.getMembers().add("member2");

    LdapEntry destination = new LdapEntry();
    AttributeModification[] modifications = mapper.mapAndComputeModifications(source, destination);
    assertNotNull(modifications);
    assertEquals(3, modifications.length); // plus 'sAMAccountName'
    assertEquals("somename", destination.getAttribute("name").getStringValue());
    assertEquals("somename", destination.getAttribute("sAMAccountName").getStringValue());
    assertTrue(destination.getAttribute("member").getStringValues()
        .contains("cn=member1,cn=Users,dc=example,dc=org"));
    assertTrue(destination.getAttribute("member").getStringValues()
        .contains("cn=member2,cn=Users,dc=example,dc=org"));
    assertFalse(destination.getAttribute("member").getStringValues()
        .contains("cn=member3,cn=Users,dc=example,dc=org"));

    source.getMembers().remove(0);
    mapper.mapAndComputeModifications(source, destination);
    assertFalse(destination.getAttribute("member").getStringValues()
        .contains("cn=member1,cn=Users,dc=example,dc=org"));
    assertTrue(destination.getAttribute("member").getStringValues()
        .contains("cn=member2,cn=Users,dc=example,dc=org"));
    assertFalse(destination.getAttribute("member").getStringValues()
        .contains("cn=member3,cn=Users,dc=example,dc=org"));
  }
}