package org.bremersee.dccon.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.stream.Stream;
import org.bremersee.data.ldaptive.LdaptiveTemplate;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.repository.ldap.DnsZoneLdapMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Christian Bremer
 */
class DnsZoneRepositoryImplTest {

  private static DomainControllerProperties properties;

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
    properties = new DomainControllerProperties();
    properties.setDefaultZone("example.org");
    properties.setDnsNodeBaseDn("ou=nodes,ou={zoneName}");
    properties.setExcludedNodeRegexList(Collections.singletonList("^_excluded\\..*$"));
    properties.setReverseZoneSuffixIp4(".in-addr.arpa");

    ldaptiveTemplate = mock(LdaptiveTemplate.class);

    DnsZoneRepositoryImpl repo = new DnsZoneRepositoryImpl(
        properties,
        ldapTemplateProvider(ldaptiveTemplate));
    repo.setDnsZoneLdapMapper(new DnsZoneLdapMapper(properties));
    dnsZoneRepository = Mockito.spy(repo);
    doNothing().when(dnsZoneRepository).doDelete(anyString());
    doNothing().when(dnsZoneRepository).doSave(anyString());
  }

  /**
   * Reset ldaptive template.
   */
  @BeforeEach
  void resetLdaptiveTemplate() {
    reset(ldaptiveTemplate);
  }

  @Test
  void isDnsReverseZone() {
    assertTrue(dnsZoneRepository.isDnsReverseZone("1.168.192.in-addr.arpa"));
    assertFalse(dnsZoneRepository.isDnsReverseZone("example.org"));
  }

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
    assertEquals(2L, dnsZoneRepository.findAll()
        .filter(zone -> zone.getName()
            .equals("example.org") || zone.getName()
            .equals("1.168.192.in-addr.arpa"))
        .count());
  }

  @Test
  void exists() {
  }

  @Test
  void findOne() {
  }

  @Test
  void save() {
  }

  @Test
  void delete() {
  }
}