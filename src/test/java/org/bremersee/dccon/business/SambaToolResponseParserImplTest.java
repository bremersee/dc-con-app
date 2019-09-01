package org.bremersee.dccon.business;

import java.util.List;
import org.bremersee.dccon.model.DnsEntry;
import org.junit.Assert;
import org.junit.Test;

/**
 * The samba tool response parser test.
 *
 * @author Christian Bremer
 */
public class SambaToolResponseParserImplTest {

  private static final String PTR_INPUT = ""
      + "  Name=217, Records=1, Children=0\n"
      + "    PTR: barsch.eixe.bremersee.org (flags=f0, serial=45622, ttl=3600)\n"
      + "  Name=218\n"
      + "CNF:77fef544-4aca-42de-8c2a-305d6cd9d909, Records=1, Children=0\n"
      + "PTR: Andreas-BT.eixe.bremersee.org (flags=f0, serial=2072, ttl=3600)\n";

  private static final SambaToolResponseParserImpl parser = new SambaToolResponseParserImpl();

  /**
   * Parse dns records.
   */
  @Test
  public void parseDnsRecords() {
    CommandExecutorResponse response = new CommandExecutorResponse(PTR_INPUT, null);
    List<DnsEntry> entries = parser.parseDnsRecords(response);
    Assert.assertTrue(entries
        .stream()
        .anyMatch(dnsEntry -> "217".equalsIgnoreCase(dnsEntry.getName())));
    Assert.assertTrue(entries
        .stream()
        .anyMatch(dnsEntry -> "218CNF:77fef544-4aca-42de-8c2a-305d6cd9d909".equalsIgnoreCase(
            dnsEntry.getName())));
  }
}