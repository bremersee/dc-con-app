package org.bremersee.dccon.repository.cli;

import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.model.DhcpLease;
import org.junit.Assert;
import org.junit.Test;

/**
 * The dhcp lease list parser test.
 *
 * @author Christian Bremer
 */
@Slf4j
public class DhcpLeaseListParserTest {

  private static final DhcpLeaseParser parser = DhcpLeaseParser.defaultParser();

  /**
   * Parse dhcp lease list.
   */
  @Test
  public void parseDhcpLeaseList() {
    String line0 = "MAC b8:xx:xx:xx:xx:xx "
        + "IP 192.168.1.109 "
        + "HOSTNAME ukelei "
        + "BEGIN 2019-08-18 11:20:33 "
        + "END 2019-08-18 11:50:33 "
        + "MANUFACTURER Apple, Inc."
        + "\n";
    String line1 = "MAC ac:xx:xx:xx:xx:yy "
        + "IP 192.168.1.188 "
        + "HOSTNAME -NA- "
        + "BEGIN 2019-08-18 11:25:48 "
        + "END 2019-08-18 11:55:48 "
        + "MANUFACTURER Super Micro Computer, Inc."
        + "\n";
    log.info("Test parsing dhcp leases response:\n{}{}", line0, line1);
    CommandExecutorResponse response = new CommandExecutorResponse(line0 + line1, null);
    List<DhcpLease> leases = parser.parse(response);
    Assert.assertNotNull(leases);
    Assert.assertEquals(2, leases.size());

    DhcpLease lease = leases.get(0);
    Assert.assertEquals("b8:xx:xx:xx:xx:xx", lease.getMac());
    Assert.assertEquals("192.168.1.109", lease.getIp());
    Assert.assertEquals("ukelei", lease.getHostname());
    Assert.assertEquals(
        "2019-08-18 11:20:33",
        lease.getBegin().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    Assert.assertEquals(
        "2019-08-18 11:50:33",
        lease.getEnd().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    Assert.assertEquals("Apple, Inc.", lease.getManufacturer());

    lease = leases.get(1);
    Assert.assertEquals("ac:xx:xx:xx:xx:yy", lease.getMac());
    Assert.assertEquals("192.168.1.188", lease.getIp());
    Assert.assertEquals("dhcp-192-168-1-188", lease.getHostname());
    Assert.assertEquals(
        "2019-08-18 11:25:48",
        lease.getBegin().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    Assert.assertEquals(
        "2019-08-18 11:55:48",
        lease.getEnd().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    Assert.assertEquals("Super Micro Computer, Inc.", lease.getManufacturer());
  }

}
