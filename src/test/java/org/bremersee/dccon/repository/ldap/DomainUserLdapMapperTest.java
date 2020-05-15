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

import static org.bremersee.data.ldaptive.LdaptiveEntryMapper.getAttributeValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Month;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainUser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.ldaptive.AttributeModification;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.ldaptive.io.IntegerValueTranscoder;
import org.ldaptive.io.StringValueTranscoder;

/**
 * The domain user ldap mapper test.
 *
 * @author Christian Bremer
 */
class DomainUserLdapMapperTest {

  private static DomainUserLdapMapper mapper;

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
    mapper = new DomainUserLdapMapper(properties);
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
  void map() {
    assertNull(mapper.map(null));

    DomainUser destination = DomainUser.builder().build();
    mapper.map(null, destination);
    assertEquals(DomainUser.builder().build(), destination);

    LdapEntry source = new LdapEntry();
    source.setDn("cn=somename,cn=Users,dc=example,dc=org");
    source.addAttribute(new LdapAttribute(
        AbstractLdapMapper.WHEN_CREATED, "20170520150034.000Z"));
    source.addAttribute(new LdapAttribute(
        AbstractLdapMapper.WHEN_CHANGED, "20180621160135.000Z"));

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

  }

  /**
   * Map and compute modifications.
   */
  @Test
  void mapAndComputeModifications() {
    DomainUser source = new DomainUser();
    source.setCreated(OffsetDateTime.now());
    source.setDisplayName("Anna Livia Plurabelle");
    source.setEmail("anna@example.org");
    source.setEnabled(true);
    source.setFirstName("Anna Livia");
    source.setGroups(Collections.singletonList("joyce"));
    source.setHomeDirectory("\\\\data\\users\\anna");
    source.setLastName("Plurabelle");
    source.setLoginShell("/bin/bash");
    source.setMobile("0123456789");
    source.setTelephoneNumber("00123456789");
    source.setUnixHomeDirectory("/home/anna");
    source.setUserName("anna");
    source.setEnabled(true);

    LdapEntry destination = new LdapEntry();

    mapper.mapAndComputeModifications(source, destination);

    StringValueTranscoder svt = new StringValueTranscoder();
    assertEquals(
        source.getDisplayName(),
        getAttributeValue(destination, "displayName", svt, null));
    assertEquals(
        source.getDisplayName(),
        getAttributeValue(destination, "gecos", svt, null));
    assertEquals(
        source.getEmail(),
        getAttributeValue(destination, "mail", svt, null));
    assertEquals(
        source.getFirstName(),
        getAttributeValue(destination, "givenName", svt, null));
    assertEquals(
        source.getHomeDirectory(),
        getAttributeValue(destination, "homeDirectory", svt, null));
    assertEquals(
        source.getLastName(),
        getAttributeValue(destination, "sn", svt, null));
    assertEquals(
        source.getLoginShell(),
        getAttributeValue(destination, "loginShell", svt, null));
    assertEquals(
        source.getMobile(),
        getAttributeValue(destination, "mobile", svt, null));
    assertEquals(
        source.getTelephoneNumber(),
        getAttributeValue(destination, "telephoneNumber", svt, null));
    assertEquals(
        source.getUnixHomeDirectory(),
        getAttributeValue(destination, "unixHomeDirectory", svt, null));
    assertEquals(
        source.getUserName(),
        getAttributeValue(destination, "name", svt, null));
    assertEquals(
        source.getUserName(),
        getAttributeValue(destination, "uid", svt, null));
    assertEquals(
        source.getUserName(),
        getAttributeValue(destination, "sAMAccountName", svt, null));

    // Groups must be set in group entity.
    // List<String> groupDns = getAttributeValuesAsList(destination, "memberOf", svt);
    // assertEquals(1, groupDns.size());
    // assertEquals("cn=joyce,cn=Users,dc=example,dc=org", groupDns.get(0));

    IntegerValueTranscoder ivt = new IntegerValueTranscoder();
    assertEquals(
        66048,
        (long) getAttributeValue(destination, "userAccountControl", ivt, null));

    source.setEnabled(false);
    source.setGroups(new ArrayList<>());

    AttributeModification[] modifications = mapper.mapAndComputeModifications(source, destination);
    assertEquals(1, modifications.length);
    assertEquals(
        66050,
        (long) getAttributeValue(destination, "userAccountControl", ivt, null));
    // groupDns = getAttributeValuesAsList(destination, "memberOf", svt);
    // Assert.assertTrue(groupDns.isEmpty());
  }
}