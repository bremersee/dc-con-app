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

import java.time.Month;
import java.time.OffsetDateTime;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DnsZone;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ldaptive.AttributeModification;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;

/**
 * The dns zone ldap mapper test.
 */
@ExtendWith(SoftAssertionsExtension.class)
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
    assertThat(mapper.getObjectClasses())
        .isEmpty();
  }

  /**
   * Map dn.
   */
  @Test
  void mapDn() {
    assertThat(mapper.mapDn(DnsZone.builder().name("example.org").build()))
        .isEqualTo("dc=example.org,cn=zones,dc=example,dc=org");
  }

  /**
   * Map.
   *
   * @param softly the soft assertions
   */
  @Test
  void map(SoftAssertions softly) {
    softly.assertThat(mapper.map(null)).isNull();

    DnsZone dnsZone = DnsZone.builder().build();
    mapper.map(null, dnsZone);
    softly.assertThat(dnsZone)
        .isEqualTo(DnsZone.builder().build());

    LdapEntry ldapEntry = new LdapEntry();
    ldapEntry.setDn("dc=example.org,cn=zones,dc=example,dc=org");
    ldapEntry.addAttributes(
        new LdapAttribute(AbstractLdapMapper.WHEN_CREATED, "20170520150034.000Z"),
        new LdapAttribute(AbstractLdapMapper.WHEN_CHANGED, "20180621160135.000Z"),
        new LdapAttribute("name", "example.org")
    );

    dnsZone = mapper.map(ldapEntry);
    softly.assertThat(dnsZone)
        .extracting(DnsZone::getDistinguishedName)
        .isEqualTo("dc=example.org,cn=zones,dc=example,dc=org");
    softly.assertThat(dnsZone)
        .extracting(DnsZone::getName)
        .isEqualTo("example.org");
    softly.assertThat(dnsZone)
        .extracting(DnsZone::getCreated)
        .extracting(OffsetDateTime::getYear)
        .isEqualTo(2017);
    softly.assertThat(dnsZone)
        .extracting(DnsZone::getCreated)
        .extracting(OffsetDateTime::getMonth)
        .isEqualTo(Month.MAY);
    softly.assertThat(dnsZone)
        .extracting(DnsZone::getCreated)
        .extracting(OffsetDateTime::getDayOfMonth)
        .isEqualTo(20);
    softly.assertThat(dnsZone)
        .extracting(DnsZone::getCreated)
        .extracting(OffsetDateTime::getHour)
        .isEqualTo(15);
    softly.assertThat(dnsZone)
        .extracting(DnsZone::getCreated)
        .extracting(OffsetDateTime::getMinute)
        .isEqualTo(0);
    softly.assertThat(dnsZone)
        .extracting(DnsZone::getCreated)
        .extracting(OffsetDateTime::getSecond)
        .isEqualTo(34);
    softly.assertThat(dnsZone)
        .extracting(DnsZone::getModified)
        .extracting(OffsetDateTime::getYear)
        .isEqualTo(2018);
    softly.assertThat(dnsZone)
        .extracting(DnsZone::getModified)
        .extracting(OffsetDateTime::getMonth)
        .isEqualTo(Month.JUNE);
    softly.assertThat(dnsZone)
        .extracting(DnsZone::getModified)
        .extracting(OffsetDateTime::getDayOfMonth)
        .isEqualTo(21);
    softly.assertThat(dnsZone)
        .extracting(DnsZone::getModified)
        .extracting(OffsetDateTime::getHour)
        .isEqualTo(16);
    softly.assertThat(dnsZone)
        .extracting(DnsZone::getModified)
        .extracting(OffsetDateTime::getMinute)
        .isEqualTo(1);
    softly.assertThat(dnsZone)
        .extracting(DnsZone::getModified)
        .extracting(OffsetDateTime::getSecond)
        .isEqualTo(35);
  }

  /**
   * Map and compute modifications.
   */
  @Test
  void mapAndComputeModifications() {
    // A dns zone ldap entry cannot be changed
    AttributeModification[] modifications = mapper.mapAndComputeModifications(
        new DnsZone(), new LdapEntry());
    assertThat(modifications)
        .hasSize(0);
  }

}