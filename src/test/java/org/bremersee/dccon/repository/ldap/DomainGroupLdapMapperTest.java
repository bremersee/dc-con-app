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

import java.time.Month;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainGroup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.ldaptive.AttributeModification;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;

/**
 * The domain group ldap mapper test.
 *
 * @author Christian Bremer
 */
class DomainGroupLdapMapperTest {

  private static DomainGroupLdapMapper mapper;

  /**
   * Init.
   */
  @BeforeAll
  static void init() {
    DomainControllerProperties properties = new DomainControllerProperties();
    properties.setGroupRdn("cn");
    properties.setGroupBaseDn("cn=Users,dc=example,dc=org");
    properties.setUserRdn("cn");
    properties.setUserBaseDn("cn=Users,dc=example,dc=org");
    mapper = new DomainGroupLdapMapper(properties);
  }

  /**
   * Gets object classes.
   */
  @Test
  void getObjectClasses() {
    assertArrayEquals(new String[0], mapper.getObjectClasses());
  }

  /**
   * Map distinguished name.
   */
  @Test
  void mapDn() {
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
  void map() {
    assertNull(mapper.map(null));

    DomainGroup destination = DomainGroup.builder().build();
    mapper.map(null, destination);
    assertEquals(DomainGroup.builder().build(), destination);

    LdapEntry source = new LdapEntry();
    source.setDn("cn=somename,cn=Users,dc=example,dc=org");
    source.addAttributes(
        new LdapAttribute(AbstractLdapMapper.WHEN_CREATED, "20170520150034.000Z"),
        new LdapAttribute(AbstractLdapMapper.WHEN_CHANGED, "20180621160135.000Z"),
        new LdapAttribute("name", "somename"),
        new LdapAttribute(
            "member",
            "cn=member1,cn=Users,dc=example,dc=org", "cn=member2,cn=Users,dc=example,dc=org")
    );

    destination = mapper.map(source);
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
  void mapAndComputeModifications() {
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