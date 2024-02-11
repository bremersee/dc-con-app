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

package org.bremersee.dccon.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.bremersee.ldaptive.LdaptiveTemplate;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.repository.ldap.DnsZoneLdapMapper;
import org.bremersee.exception.ServiceException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.ObjectProvider;

/**
 * The dns zone repository impl test.
 *
 * @author Christian Bremer
 */
@ExtendWith(SoftAssertionsExtension.class)
class DnsZoneRepositoryImplTest {

  private static LdaptiveTemplate ldaptiveTemplate;

  private static DnsZoneRepositoryImpl dnsZoneRepository;

  private static ObjectProvider<LdaptiveTemplate> ldapTemplateProvider(LdaptiveTemplate template) {
    //noinspection unchecked
    ObjectProvider<LdaptiveTemplate> provider = mock(ObjectProvider.class);
    Mockito.when(provider.getIfAvailable()).thenReturn(template);
    return provider;
  }

  /**
   * Init.
   */
  @BeforeAll
  static void init() {
    DomainControllerProperties properties = new DomainControllerProperties();
    properties.setDefaultZone("example.org");
    properties.setDnsNodeBaseDn("ou=nodes,ou={zoneName}");
    properties.setExcludedNodeRegexList(Collections.singletonList("^_excluded\\..*$"));
    properties.setExcludedZoneRegexList(Collections.singletonList("^_excluded\\..*$"));
    properties.setReverseZoneSuffixIp4(".in-addr.arpa");

    ldaptiveTemplate = mock(LdaptiveTemplate.class);

    DnsZoneRepositoryImpl repo = new DnsZoneRepositoryImpl(
        properties,
        ldapTemplateProvider(ldaptiveTemplate));
    repo.setDnsZoneLdapMapper(new DnsZoneLdapMapper(properties));
    dnsZoneRepository = Mockito.spy(repo);
    doNothing().when(dnsZoneRepository).doDelete(anyString());
  }

  /**
   * Reset ldaptive template.
   */
  @BeforeEach
  void resetLdaptiveTemplate() {
    reset(ldaptiveTemplate);
  }

  /**
   * Is dns reverse zone.
   *
   * @param softly the soft assertions
   */
  @Test
  void isDnsReverseZone(SoftAssertions softly) {
    softly
        .assertThat(dnsZoneRepository.isDnsReverseZone("1.168.192.in-addr.arpa"))
        .isTrue();
    softly
        .assertThat(dnsZoneRepository.isDnsReverseZone("example.org"))
        .isFalse();
  }

  /**
   * Find all.
   */
  @Test
  void findAll() {
    DnsZone dnsZone0 = DnsZone.builder()
        .name("example.org")
        .defaultZone(true)
        .reverseZone(false)
        .build();
    DnsZone dnsZone1 = DnsZone.builder()
        .name("1.168.192.in-addr.arpa")
        .defaultZone(false)
        .reverseZone(true)
        .build();
    when(ldaptiveTemplate.findAll(any(), any()))
        .thenAnswer((Answer<Stream<DnsZone>>) invocationOnMock -> Stream.of(dnsZone0, dnsZone1));
    assertThat(dnsZoneRepository.findAll())
        .map(DnsZone::getName)
        .containsExactlyInAnyOrder("example.org", "1.168.192.in-addr.arpa");
  }

  /**
   * Exists.
   */
  @Test
  void exists() {
    DnsZone expected = DnsZone.builder().name("example.org").build();
    when(ldaptiveTemplate.exists(any(), any())).thenReturn(true);
    assertThat(dnsZoneRepository.exists(expected.getName()))
        .isTrue();
  }

  /**
   * Find one.
   */
  @Test
  void findOne() {
    DnsZone expected = DnsZone.builder().name("example.org").build();
    when(ldaptiveTemplate.findOne(any(), any())).thenReturn(Optional.of(expected));
    Optional<DnsZone> actual = dnsZoneRepository.findOne("example.org");
    assertThat(actual)
        .map(DnsZone::getName)
        .hasValue(expected.getName());
  }

  /**
   * Save.
   */
  @Test
  void save() {
    final DnsZone expected = DnsZone.builder()
        .name("domain.org")
        .build();
    doReturn(expected).when(dnsZoneRepository).doSave(anyString());
    DnsZone actual = dnsZoneRepository.save("domain.org");
    assertThat(actual)
        .isNotNull()
        .extracting(DnsZone::getName)
        .isEqualTo("domain.org");
  }

  /**
   * Save and expect service exception.
   */
  @Test
  void saveAndExpectServiceException() {
    assertThatExceptionOfType(ServiceException.class)
        .isThrownBy(() -> dnsZoneRepository.save("_excluded.zone"));
  }

  /**
   * Delete and expect true.
   */
  @Test
  void deleteAndExpectTrue() {
    when(ldaptiveTemplate.exists(any(), any())).thenReturn(true);
    assertThat(dnsZoneRepository.delete("example.org"))
        .isTrue();
    verify(dnsZoneRepository).doDelete(anyString());
  }

  /**
   * Delete and expect false.
   */
  @Test
  void deleteAndExpectFalse() {
    when(ldaptiveTemplate.exists(any(), any())).thenReturn(false);
    assertThat(dnsZoneRepository.delete("example.org"))
        .isFalse();
    verify(dnsZoneRepository, never()).doDelete(anyString());
  }
}