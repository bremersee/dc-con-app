package org.bremersee.dccon.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.junit.Test;

/**
 * The dns node repository test.
 *
 * @author Christian Bremer
 */
public class DnsNodeRepositoryTest {

  /**
   * Ip 4 matches dns zone.
   */
  @Test
  public void ip4MatchesDnsZone() {
    DomainControllerProperties properties = new DomainControllerProperties();
    DnsNodeRepositoryImpl repository = new DnsNodeRepositoryImpl(properties, null, null);

    assertTrue(repository.ip4MatchesDnsZone(
        "192.168.1.124",
        "1.168.192" + properties.getReverseZoneSuffixIp4()));
    assertFalse(repository.ip4MatchesDnsZone(
        "192.168.11.124",
        "1.168.192" + properties.getReverseZoneSuffixIp4()));
  }

  /**
   * Gets dns node name by ip 4.
   */
  @Test
  public void getDnsNodeNameByIp4() {
    DomainControllerProperties properties = new DomainControllerProperties();
    DnsNodeRepositoryImpl repository = new DnsNodeRepositoryImpl(properties, null, null);

    Optional<String> nodeName = repository.getDnsNodeNameByIp4(
        "192.168.1.124",
        "1.168.192" + properties.getReverseZoneSuffixIp4());
    assertTrue(nodeName.isPresent());
    assertEquals(
        "124",
        nodeName.get());
  }

  /**
   * Gets dns node name by fqdn.
   */
  @Test
  public void getDnsNodeNameByFqdn() {
    DomainControllerProperties properties = new DomainControllerProperties();
    DnsNodeRepositoryImpl repository = new DnsNodeRepositoryImpl(properties, null, null);

    Optional<String> nodeName = repository.getDnsNodeNameByFqdn(
        "pluto.eixe.bremersee.org",
        "eixe.bremersee.org");
    assertTrue(nodeName.isPresent());
    assertEquals(
        "pluto",
        nodeName.get());
  }

  /**
   * Split ip 4.
   */
  @Test
  public void splitIp4() {
    DomainControllerProperties properties = new DomainControllerProperties();
    DnsNodeRepositoryImpl repository = new DnsNodeRepositoryImpl(properties, null, null);

    String[] parts = repository.splitIp4(
        "192.168.1.123",
        "1.168.192" + properties.getReverseZoneSuffixIp4());
    assertNotNull(parts);
    assertEquals(2, parts.length);
    assertEquals("192.168.1", parts[0]);
    assertEquals("123", parts[1]);

    parts = repository.splitIp4(
        "192.168.1.123",
        "168.192" + properties.getReverseZoneSuffixIp4());
    assertNotNull(parts);
    assertEquals(2, parts.length);
    assertEquals("192.168", parts[0]);
    assertEquals("1.123", parts[1]);

    parts = repository.splitIp4(
        "192.168.11.123",
        "1.168.192" + properties.getReverseZoneSuffixIp4());
    assertNull(parts);
  }
}