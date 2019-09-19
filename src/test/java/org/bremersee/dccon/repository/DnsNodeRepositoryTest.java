package org.bremersee.dccon.repository;

import org.bremersee.dccon.config.DomainControllerProperties;
import org.junit.Assert;
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

    Assert.assertTrue(repository.ip4MatchesDnsZone(
        "192.168.1.124",
        "1.168.192" + properties.getReverseZoneSuffixIp4()));
    Assert.assertFalse(repository.ip4MatchesDnsZone(
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

    Assert.assertEquals(
        "124",
        repository.getDnsNodeNameByIp4(
            "192.168.1.124",
            "1.168.192" + properties.getReverseZoneSuffixIp4()));
  }

  /**
   * Gets dns node name by fqdn.
   */
  @Test
  public void getDnsNodeNameByFqdn() {
    DomainControllerProperties properties = new DomainControllerProperties();
    DnsNodeRepositoryImpl repository = new DnsNodeRepositoryImpl(properties, null, null);

    Assert.assertEquals(
        "pluto",
        repository.getDnsNodeNameByFqdn(
            "pluto.eixe.bremersee.org",
            "eixe.bremersee.org"));
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
    Assert.assertNotNull(parts);
    Assert.assertEquals(2, parts.length);
    Assert.assertEquals("192.168.1", parts[0]);
    Assert.assertEquals("123", parts[1]);

    parts = repository.splitIp4(
        "192.168.1.123",
        "168.192" + properties.getReverseZoneSuffixIp4());
    Assert.assertNotNull(parts);
    Assert.assertEquals(2, parts.length);
    Assert.assertEquals("192.168", parts[0]);
    Assert.assertEquals("1.123", parts[1]);

    parts = repository.splitIp4(
        "192.168.11.123",
        "1.168.192" + properties.getReverseZoneSuffixIp4());
    Assert.assertNotNull(parts);
    Assert.assertEquals(2, parts.length);
    Assert.assertEquals("", parts[0]);
    Assert.assertEquals("", parts[1]);
  }
}