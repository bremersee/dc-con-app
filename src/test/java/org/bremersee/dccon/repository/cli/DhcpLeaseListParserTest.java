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

package org.bremersee.dccon.repository.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.model.DhcpLease;
import org.junit.jupiter.api.Test;

/**
 * The dhcp lease list parser test.
 *
 * @author Christian Bremer
 */
@Slf4j
class DhcpLeaseListParserTest {

  /**
   * Parse empty dhcp lease list.
   */
  @Test
  void parseEmptyDhcpLeaseList() {
    DhcpLeaseParser parser = DhcpLeaseParser
        .defaultParser((mac, ip) -> "dhcp-" + ip.replace(".", "-"));
    CommandExecutorResponse response = new CommandExecutorResponse(null, null);
    List<DhcpLease> expected = Collections.emptyList();
    List<DhcpLease> actual = parser.parse(response);
    assertThat(actual)
        .isEqualTo(expected);
  }

  /**
   * Parse dhcp lease list.
   */
  @Test
  void parseDhcpLeaseList() {
    DhcpLeaseParser parser = DhcpLeaseParser.defaultParser();
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
    assertThat(leases)
        .containsExactlyInAnyOrder(
            DhcpLease.builder()
                .mac("b8:xx:xx:xx:xx:xx")
                .ip("192.168.1.109")
                .hostname("ukelei")
                .begin(OffsetDateTime.of(
                    LocalDateTime.parse(
                        "2019-08-18 11:20:33",
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    ZoneOffset.UTC))
                .end(OffsetDateTime.of(
                    LocalDateTime.parse(
                        "2019-08-18 11:50:33",
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    ZoneOffset.UTC))
                .manufacturer("Apple, Inc.")
                .build(),
            DhcpLease.builder()
                .mac("ac:xx:xx:xx:xx:yy")
                .ip("192.168.1.188")
                .hostname("dhcp-192-168-1-188")
                .begin(OffsetDateTime.of(
                    LocalDateTime.parse(
                        "2019-08-18 11:25:48",
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    ZoneOffset.UTC))
                .end(OffsetDateTime.of(
                    LocalDateTime.parse(
                        "2019-08-18 11:55:48",
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    ZoneOffset.UTC))
                .manufacturer("Super Micro Computer, Inc.")
                .build());
  }

}
