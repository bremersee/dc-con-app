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
import static org.bremersee.ldaptive.LdaptiveEntryMapper.getAttributeValue;

import java.time.Month;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainUser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ldaptive.AttributeModification;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.ldaptive.transcode.IntegerValueTranscoder;
import org.ldaptive.transcode.StringValueTranscoder;

/**
 * The domain user ldap mapper test.
 *
 * @author Christian Bremer
 */
@ExtendWith(SoftAssertionsExtension.class)
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
    assertThat(mapper.getObjectClasses())
        .isEmpty();
  }

  /**
   * Map distinguished name.
   */
  @Test
  void mapDn() {
    DomainUser domainUser = new DomainUser();
    domainUser.setUserName("somename");
    String dn = mapper.mapDn(domainUser);
    assertThat(dn)
        .isEqualTo("cn=somename,cn=Users,dc=example,dc=org");
  }

  /**
   * Map ldap entry.
   *
   * @param softly the soft assertions
   */
  @Test
  void map(SoftAssertions softly) {
    softly.assertThat(mapper.map(null)).isNull();

    DomainUser destination = DomainUser.builder().build();
    mapper.map(null, destination);
    softly.assertThat(destination)
        .isEqualTo(DomainUser.builder().build());

    LdapEntry source = new LdapEntry();
    source.setDn("cn=somename,cn=Users,dc=example,dc=org");
    source.addAttributes(
        new LdapAttribute(AbstractLdapMapper.WHEN_CREATED, "20170520150034.000Z"),
        new LdapAttribute(AbstractLdapMapper.WHEN_CHANGED, "20180621160135.000Z")
    );

    destination = mapper.map(source);
    softly.assertThat(destination)
        .extracting(DomainUser::getDistinguishedName)
        .isEqualTo("cn=somename,cn=Users,dc=example,dc=org");
    softly.assertThat(destination)
        .extracting(DomainUser::getCreated)
        .extracting(OffsetDateTime::getYear)
        .isEqualTo(2017);
    softly.assertThat(destination)
        .extracting(DomainUser::getCreated)
        .extracting(OffsetDateTime::getMonth)
        .isEqualTo(Month.MAY);
    softly.assertThat(destination)
        .extracting(DomainUser::getCreated)
        .extracting(OffsetDateTime::getDayOfMonth)
        .isEqualTo(20);
    softly.assertThat(destination)
        .extracting(DomainUser::getCreated)
        .extracting(OffsetDateTime::getHour)
        .isEqualTo(15);
    softly.assertThat(destination)
        .extracting(DomainUser::getCreated)
        .extracting(OffsetDateTime::getMinute)
        .isEqualTo(0);
    softly.assertThat(destination)
        .extracting(DomainUser::getCreated)
        .extracting(OffsetDateTime::getSecond)
        .isEqualTo(34);

    softly.assertThat(destination)
        .extracting(DomainUser::getModified)
        .extracting(OffsetDateTime::getYear)
        .isEqualTo(2018);
    softly.assertThat(destination)
        .extracting(DomainUser::getModified)
        .extracting(OffsetDateTime::getMonth)
        .isEqualTo(Month.JUNE);
    softly.assertThat(destination)
        .extracting(DomainUser::getModified)
        .extracting(OffsetDateTime::getDayOfMonth)
        .isEqualTo(21);
    softly.assertThat(destination)
        .extracting(DomainUser::getModified)
        .extracting(OffsetDateTime::getHour)
        .isEqualTo(16);
    softly.assertThat(destination)
        .extracting(DomainUser::getModified)
        .extracting(OffsetDateTime::getMinute)
        .isEqualTo(1);
    softly.assertThat(destination)
        .extracting(DomainUser::getModified)
        .extracting(OffsetDateTime::getSecond)
        .isEqualTo(35);
  }

  /**
   * Map and compute modifications.
   *
   * @param softly the soft assertions
   */
  @Test
  void mapAndComputeModifications(SoftAssertions softly) {
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
    softly.assertThat(getAttributeValue(destination, "displayName", svt, null))
        .isEqualTo(source.getDisplayName());
    softly.assertThat(getAttributeValue(destination, "gecos", svt, null))
        .isEqualTo(source.getDisplayName());
    softly.assertThat(getAttributeValue(destination, "mail", svt, null))
        .isEqualTo(source.getEmail());
    softly.assertThat(getAttributeValue(destination, "givenName", svt, null))
        .isEqualTo(source.getFirstName());
    softly.assertThat(getAttributeValue(destination, "homeDirectory", svt, null))
        .isEqualTo(source.getHomeDirectory());
    softly.assertThat(getAttributeValue(destination, "sn", svt, null))
        .isEqualTo(source.getLastName());
    softly.assertThat(getAttributeValue(destination, "loginShell", svt, null))
        .isEqualTo(source.getLoginShell());
    softly.assertThat(getAttributeValue(destination, "mobile", svt, null))
        .isEqualTo(source.getMobile());
    softly.assertThat(getAttributeValue(destination, "telephoneNumber", svt, null))
        .isEqualTo(source.getTelephoneNumber());
    softly.assertThat(getAttributeValue(destination, "unixHomeDirectory", svt, null))
        .isEqualTo(source.getUnixHomeDirectory());
    softly.assertThat(getAttributeValue(destination, "name", svt, null))
        .isEqualTo(source.getUserName());
    softly.assertThat(getAttributeValue(destination, "uid", svt, null))
        .isEqualTo(source.getUserName());
    softly.assertThat(getAttributeValue(destination, "sAMAccountName", svt, null))
        .isEqualTo(source.getUserName());

    // Groups must be set in group entity.
    // List<String> groupDns = getAttributeValuesAsList(destination, "memberOf", svt);
    // assertEquals(1, groupDns.size());
    // assertEquals("cn=joyce,cn=Users,dc=example,dc=org", groupDns.get(0));

    IntegerValueTranscoder ivt = new IntegerValueTranscoder();
    softly.assertThat(getAttributeValue(destination, "userAccountControl", ivt, null))
        .isEqualTo(66048);

    source.setEnabled(false);
    source.setGroups(new ArrayList<>());

    AttributeModification[] modifications = mapper.mapAndComputeModifications(source, destination);
    softly.assertThat(modifications)
        .hasSize(1);
    softly.assertThat(getAttributeValue(destination, "userAccountControl", ivt, null))
        .isEqualTo(66050);
  }
}