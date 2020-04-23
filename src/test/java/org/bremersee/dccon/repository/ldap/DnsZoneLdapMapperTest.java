/*
 * Copyright 2019 the original author or authors.
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Month;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DnsZone;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.ldaptive.AttributeModification;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;

/**
 * The dns zone ldap mapper test.
 *
 * @author Christian Bremer
 */
class DnsZoneLdapMapperTest {

  private static DnsZoneLdapMapper mapper;

  /**
   * Init.
   */
  @BeforeAll
  static void init() {
    DomainControllerProperties properties = new DomainControllerProperties();
    properties.setDnsZoneBaseDn("cn=zones,dc=example,dc=org");
    properties.setDnsZoneRdn("dc");
    mapper = new DnsZoneLdapMapper(properties);
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
    assertEquals("dc=example.org,cn=zones,dc=example,dc=org",
        mapper.mapDn(DnsZone.builder().name("example.org").build()));
  }

  /**
   * Map.
   */
  @Test
  void map() {
    assertNull(mapper.map(null));

    DnsZone dnsZone = DnsZone.builder().build();
    mapper.map(null, dnsZone);
    assertEquals(DnsZone.builder().build(), dnsZone);

    LdapEntry ldapEntry = new LdapEntry();
    ldapEntry.setDn("dc=example.org,cn=zones,dc=example,dc=org");
    ldapEntry.addAttribute(new LdapAttribute(
        AbstractLdapMapper.WHEN_CREATED, "20170520150034.000Z"));
    ldapEntry.addAttribute(new LdapAttribute(
        AbstractLdapMapper.WHEN_CHANGED, "20180621160135.000Z"));
    ldapEntry.addAttribute(new LdapAttribute("name", "example.org"));

    dnsZone = mapper.map(ldapEntry);
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
  void mapAndComputeModifications() {
    // A dns zone ldap entry cannot be changed
    AttributeModification[] modifications = mapper.mapAndComputeModifications(
        new DnsZone(), new LdapEntry());
    assertNotNull(modifications);
    assertEquals(0, modifications.length);
  }

}