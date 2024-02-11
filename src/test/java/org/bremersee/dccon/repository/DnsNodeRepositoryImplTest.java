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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.bremersee.ldaptive.LdaptiveTemplate;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DhcpLease;
import org.bremersee.dccon.model.DnsNode;
import org.bremersee.dccon.model.DnsRecord;
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.model.UnknownFilter;
import org.bremersee.dccon.repository.ldap.DnsNodeLdapMapper;
import org.bremersee.exception.ServiceException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.ObjectProvider;

/**
 * The dns node repository impl test.
 *
 * @author Christian Bremer
 */
@ExtendWith(SoftAssertionsExtension.class)
class DnsNodeRepositoryImplTest {

  private static DomainControllerProperties properties;

  private static DhcpRepository dhcpRepository;

  private static LdaptiveTemplate ldaptiveTemplate;

  private static DnsZoneRepository dnsZoneRepository;

  private static DnsNodeRepositoryImpl dnsNodeRepository;

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
    properties = new DomainControllerProperties();
    properties.setDefaultZone("example.org");
    properties.setDnsNodeBaseDn("ou=nodes,ou={zoneName}");
    properties.setExcludedNodeRegexList(Collections.singletonList("^_excluded\\..*$"));
    properties.setReverseZoneSuffixIp4(".in-addr.arpa");

    dhcpRepository = mock(DhcpRepository.class);
    DhcpLease lease = DhcpLease.builder()
        .ip("192.168.1.123")
        .hostname("node0")
        .begin(OffsetDateTime.now())
        .end(OffsetDateTime.now())
        .mac("xx:etc")
        .manufacturer("JUnit")
        .build();
    when(dhcpRepository.findActiveByIp())
        .thenReturn(Collections.singletonMap("192.168.1.123", lease));
    when(dhcpRepository.findActiveByHostName())
        .thenReturn(Collections.singletonMap("NODE0", lease));

    DnsZone dnsZone0 = DnsZone.builder().defaultZone(true).name("example.org").build();
    DnsZone dnsZone1 = DnsZone.builder().defaultZone(false).name("1.168.192.in-addr.arpa").build();
    dnsZoneRepository = mock(DnsZoneRepository.class);
    when(dnsZoneRepository.findAll())
        .thenAnswer((Answer<Stream<DnsZone>>) invocationOnMock -> Stream.of(dnsZone0, dnsZone1));
    when(dnsZoneRepository.isDnsReverseZone(anyString()))
        .then((Answer<Boolean>) invocationOnMock -> invocationOnMock
            .getArgument(0).toString().endsWith(properties.getReverseZoneSuffixIp4()));
    when(dnsZoneRepository.exists(anyString()))
        .then((Answer<Boolean>) invocationOnMock -> invocationOnMock
            .getArgument(0).equals("example.org") || invocationOnMock
            .getArgument(0).equals("1.168.192.in-addr.arpa"));
    when(dnsZoneRepository.findNonDnsReverseZones())
        .thenAnswer((Answer<Stream<DnsZone>>) invocationOnMock -> Stream.of(dnsZone0));
    when(dnsZoneRepository.findDnsReverseZones())
        .thenAnswer((Answer<Stream<DnsZone>>) invocationOnMock -> Stream.of(dnsZone1));

    ldaptiveTemplate = mock(LdaptiveTemplate.class);
    dnsNodeRepository = new DnsNodeRepositoryImpl(
        properties,
        ldapTemplateProvider(ldaptiveTemplate),
        dhcpRepository,
        dnsZoneRepository);
    dnsNodeRepository
        .setDnsNodeLdapMapperProvider((zoneName, unknownFilter) -> new DnsNodeLdapMapper(
            properties, zoneName, unknownFilter));

    dnsNodeRepository = Mockito.spy(dnsNodeRepository);
    doNothing().when(dnsNodeRepository).add(anyString(), anyString(), anyCollection());
  }

  /**
   * Reset ldaptive template.
   */
  @BeforeEach
  void resetLdaptiveTemplate() {
    reset(ldaptiveTemplate);
  }

  /**
   * Keep dhcp lease caches up to date.
   */
  @Test
  void keepDhcpLeaseCachesUpToDate() {
    dnsNodeRepository.keepDhcpLeaseCachesUpToDate();
    verify(dhcpRepository, atLeastOnce()).findActiveByIp();
    verify(dhcpRepository, atLeastOnce()).findActiveByHostName();
  }

  /**
   * Find all.
   */
  @Test
  void findAll() {
    DnsNode node0 = DnsNode.builder()
        .name("node0")
        .records(Collections.singleton(DnsRecord.builder()
            .recordType("A")
            .recordValue("192.168.1.123")
            .build()))
        .build();
    when(ldaptiveTemplate.findAll(any(), any()))
        .thenAnswer((Answer<Stream<DnsNode>>) invocationOnMock -> Stream.of(node0));
    Stream<DnsNode> actual = dnsNodeRepository
        .findAll("example.org", UnknownFilter.ALL, null);
    assertThat(actual)
        .map(DnsNode::getName)
        .contains(node0.getName());
  }

  /**
   * Find all with name query.
   */
  @Test
  void findAllWithNameQuery() {
    DnsNode node0 = DnsNode.builder()
        .name("node0")
        .records(Collections.singleton(DnsRecord.builder()
            .recordType("A")
            .recordValue("192.168.1.123")
            .build()))
        .build();
    when(ldaptiveTemplate.findAll(any(), any()))
        .thenAnswer((Answer<Stream<DnsNode>>) invocationOnMock -> Stream.of(node0));
    Stream<DnsNode> actual = dnsNodeRepository
        .findAll("example.org", UnknownFilter.ALL, "node0");
    assertThat(actual)
        .map(DnsNode::getName)
        .contains(node0.getName());
  }

  /**
   * Find all with ip query.
   */
  @Test
  void findAllWithIpQuery() {
    DnsNode node0 = DnsNode.builder()
        .name("node0")
        .records(Collections.singleton(DnsRecord.builder()
            .recordType("A")
            .recordValue("192.168.1.123")
            .build()))
        .build();
    when(ldaptiveTemplate.findAll(any(), any()))
        .thenAnswer((Answer<Stream<DnsNode>>) invocationOnMock -> Stream.of(node0));
    Stream<DnsNode> actual = dnsNodeRepository
        .findAll("example.org", UnknownFilter.ALL, "192.168.1.123");
    assertThat(actual)
        .map(DnsNode::getName)
        .contains(node0.getName());
  }

  /**
   * Exists.
   */
  @Test
  void exists() {
    when(ldaptiveTemplate.exists(any(), any())).thenReturn(true);
    boolean actual = dnsNodeRepository.exists("example.org", "node0", UnknownFilter.ALL);
    assertThat(actual)
        .isTrue();
  }

  /**
   * Find one.
   */
  @Test
  void findOne() {
    DnsNode node0 = DnsNode.builder()
        .name("node0")
        .records(Collections.singleton(DnsRecord.builder()
            .recordType("A")
            .recordValue("192.168.1.123")
            .build()))
        .build();
    when(ldaptiveTemplate.findOne(any(), any())).thenReturn(Optional.of(node0));
    Optional<DnsNode> actual = dnsNodeRepository
        .findOne("example.org", "node0", UnknownFilter.ALL);
    assertThat(actual)
        .map(DnsNode::getName)
        .hasValue("node0");
  }

  /**
   * Save and expect node name is not allowed.
   */
  @Test
  void saveAndExpectNodeNameIsNotAllowed() {
    DnsNode excluded = DnsNode.builder().name("_excluded.node0").build();
    assertThatExceptionOfType(ServiceException.class)
        .isThrownBy(() -> dnsNodeRepository.save("example.org", excluded));
  }

  /**
   * Save.
   */
  @Test
  void save() {
    DnsNode expected = DnsNode.builder()
        .name("node0")
        .records(Collections.singleton(DnsRecord.builder()
            .recordValue("node0")
            .recordType("A")
            .recordValue("192.168.1.123")
            .build()))
        .build();
    DnsNode input = expected.toBuilder()
        .records(Collections.singleton(DnsRecord.builder()
            .recordValue("node0")
            .recordType("A")
            .recordValue("192.168.1.122")
            .build()))
        .build();
    DnsNode ptr = expected.toBuilder()
        .records(Collections.singleton(DnsRecord.builder()
            .recordValue("123")
            .recordType("PTR")
            .recordValue("node0")
            .build()))
        .build();
    when(ldaptiveTemplate.save(any(), any()))
        .thenAnswer((Answer<DnsNode>) invocationOnMock -> {
          DnsNode input1 = invocationOnMock.getArgument(0);
          return input1.getName().contains("node") ? expected : ptr;
        });
    when(ldaptiveTemplate.findOne(any(), any())).thenReturn(Optional.of(expected));

    Optional<DnsNode> actual = dnsNodeRepository.save("example.org", input);
    assertThat(actual)
        .isPresent();
  }

  /**
   * Delete and expect node name is not allowed.
   */
  @Test
  void deleteAndExpectNodeNameIsNotAllowed() {
    DnsNode excluded = DnsNode.builder().name("_excluded.node0").build();
    assertThatExceptionOfType(ServiceException.class)
        .isThrownBy(() -> dnsNodeRepository.delete("example.org", excluded));
  }

  /**
   * Delete.
   */
  @Test
  void delete() {
    DnsNode node0 = DnsNode.builder()
        .name("node0")
        .records(Collections.singleton(DnsRecord.builder()
            .recordType("A")
            .recordValue("192.168.1.123")
            .build()))
        .build();
    when(ldaptiveTemplate.findOne(any(), any())).thenReturn(Optional.of(node0));
    dnsNodeRepository.delete("example.org", node0);
    verify(ldaptiveTemplate).remove(any(), any());
  }

  /**
   * Delete by name.
   */
  @Test
  void deleteByName() {
    DnsNode node0 = DnsNode.builder().name("node0").build();
    when(ldaptiveTemplate.findOne(any(), any())).thenReturn(Optional.of(node0));
    dnsNodeRepository.delete("example.org", "node0");
    verify(ldaptiveTemplate).findOne(any(), any());
    verify(ldaptiveTemplate).remove(any(), any());
  }

  /**
   * Delete all.
   */
  @Test
  void deleteAll() {
    DnsNode node0 = DnsNode.builder().name("node0").build();
    when(ldaptiveTemplate.findAll(any(), any()))
        .thenAnswer((Answer<Stream<DnsNode>>) invocationOnMock -> Stream.of(node0));
    dnsNodeRepository.deleteAll("example.org");
    verify(ldaptiveTemplate).remove(any(), any());
  }

  /**
   * Delete all by name.
   */
  @Test
  void deleteAllByName() {
    DnsNode node0 = DnsNode.builder().name("node0").build();
    when(ldaptiveTemplate.findOne(any(), any())).thenReturn(Optional.of(node0));
    dnsNodeRepository.deleteAll("example.org", Collections.singletonList("node0"));
    verify(ldaptiveTemplate).remove(any(), any());
  }

  /**
   * Ip 4 matches dns zone.
   *
   * @param softly the soft assertions
   */
  @Test
  void ip4MatchesDnsZone(SoftAssertions softly) {
    DomainControllerProperties properties = new DomainControllerProperties();
    DnsNodeRepositoryImpl repository = new DnsNodeRepositoryImpl(
        properties,
        ldapTemplateProvider(ldaptiveTemplate),
        dhcpRepository,
        dnsZoneRepository);

    softly
        .assertThat(repository
            .ip4MatchesDnsZone(
                "192.168.1.124",
                "1.168.192" + properties.getReverseZoneSuffixIp4()))
        .isTrue();
    softly
        .assertThat(repository
            .ip4MatchesDnsZone(
                "192.168.11.124",
                "1.168.192" + properties.getReverseZoneSuffixIp4()))
        .isFalse();
  }

  /**
   * Gets dns node name by ip 4.
   */
  @Test
  void getDnsNodeNameByIp4() {
    DomainControllerProperties properties = new DomainControllerProperties();
    DnsNodeRepositoryImpl repository = new DnsNodeRepositoryImpl(
        properties,
        ldapTemplateProvider(ldaptiveTemplate),
        dhcpRepository,
        dnsZoneRepository);

    Optional<String> nodeName = repository.getDnsNodeNameByIp4(
        "192.168.1.124",
        "1.168.192" + properties.getReverseZoneSuffixIp4());
    assertThat(nodeName)
        .hasValue("124");
  }

  /**
   * Gets dns node name by fqdn.
   */
  @Test
  void getDnsNodeNameByFqdn() {
    DomainControllerProperties properties = new DomainControllerProperties();
    DnsNodeRepositoryImpl repository = new DnsNodeRepositoryImpl(
        properties,
        ldapTemplateProvider(ldaptiveTemplate),
        dhcpRepository,
        dnsZoneRepository);

    Optional<String> nodeName = repository.getDnsNodeNameByFqdn(
        "pluto.eixe.bremersee.org",
        "eixe.bremersee.org");
    assertThat(nodeName)
        .hasValue("pluto");
  }

  /**
   * Split ip 4.
   *
   * @param softly the soft assertions
   */
  @Test
  void splitIp4(SoftAssertions softly) {
    DomainControllerProperties properties = new DomainControllerProperties();
    DnsNodeRepositoryImpl repository = new DnsNodeRepositoryImpl(
        properties,
        ldapTemplateProvider(ldaptiveTemplate),
        dhcpRepository,
        dnsZoneRepository);

    String[] parts = repository.splitIp4(
        "192.168.1.123",
        "1.168.192" + properties.getReverseZoneSuffixIp4());
    softly.assertThat(parts)
        .containsExactly("192.168.1", "123");

    parts = repository.splitIp4(
        "192.168.1.123",
        "168.192" + properties.getReverseZoneSuffixIp4());
    softly.assertThat(parts)
        .containsExactly("192.168", "1.123");

    parts = repository.splitIp4(
        "192.168.11.123",
        "1.168.192" + properties.getReverseZoneSuffixIp4());
    softly.assertThat(parts)
        .isNull();
  }
}