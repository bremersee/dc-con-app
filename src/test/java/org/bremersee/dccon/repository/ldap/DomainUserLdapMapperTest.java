package org.bremersee.dccon.repository.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.Month;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainUser;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;

/**
 * The domain user ldap mapper test.
 *
 * @author Christian Bremer
 */
public class DomainUserLdapMapperTest {

  private static DomainUserLdapMapper mapper;

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
    mapper = new DomainUserLdapMapper(properties);
  }

  /**
   * Map distinguished name.
   */
  @Test
  public void mapDn() {
    DomainUser domainUser = new DomainUser();
    domainUser.setUserName("somename");
    String dn = mapper.mapDn(domainUser);
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
        AbstractLdapMapper.WHEN_CREATED, "20170520150034"));
    source.addAttribute(new LdapAttribute(
        AbstractLdapMapper.WHEN_CHANGED, "20180621160135"));

    DomainUser destination = mapper.map(source);
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

  }

  /**
   * Map and compute modifications.
   */
  @Test
  public void mapAndComputeModifications() {
  }
}