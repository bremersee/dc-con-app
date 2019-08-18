/*
 * Copyright 2019 the original author or authors.
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

package org.bremersee.dccon.business;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.model.DhcpLease;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * The dhcp lease list parser implementation.
 *
 * @author Christian Bremer
 */
@Component
@Slf4j
public class DhcpLeaseListParserImpl implements DhcpLeaseListParser {

  private static final String MAC = "MAC ";

  private static final String IP = " IP ";

  private static final String HOSTNAME = " HOSTNAME ";

  private static final String BEGIN = " BEGIN ";

  private static final String END = " END ";

  private static final String MANUFACTURER = " MANUFACTURER ";

  @Override
  public List<DhcpLease> parseDhcpLeaseList(@NotNull final CommandExecutorResponse response) {
    final String output = response.getOutput();
    if (!StringUtils.hasText(output)) {
      log.error("Dhcp lease list command did not produce output. Error is [{}].",
          response.getError());
      return Collections.emptyList();
    }
    if (log.isDebugEnabled()) {
      log.debug("Parsing dhcp lease list:\n{}", output);
    }
    try (final BufferedReader reader = new BufferedReader(new StringReader(output))) {
      return parseDhcpLeaseList(reader);

    } catch (IOException e) {
      log.error("Parsing dhcp lease list failed:\n" + output + "\n", e);
      return Collections.emptyList();
    }
  }

  private List<DhcpLease> parseDhcpLeaseList(final BufferedReader reader) throws IOException {
    final List<DhcpLease> leases = new ArrayList<>();
    String line;
    while ((line = reader.readLine()) != null) {
      line = line.trim();
      final String mac = findDhcpLeasePart(line, MAC, IP);
      final String ip = findDhcpLeasePart(line, IP, HOSTNAME);
      final String hostname = findDhcpLeasePart(line, HOSTNAME, BEGIN);
      final String begin = findDhcpLeasePart(line, BEGIN, END);
      final String end = findDhcpLeasePart(line, END, MANUFACTURER);
      final String manufacturer = findDhcpLeasePart(line, MANUFACTURER, null);
      if (mac != null && ip != null && hostname != null && begin != null && end != null) {
        final DhcpLease lease = new DhcpLease(
            mac,
            ip,
            hostname,
            parseDhcpLeaseTime(begin),
            parseDhcpLeaseTime(end),
            manufacturer);
        leases.add(lease);
      }
    }
    return leases;
  }

  private String findDhcpLeasePart(String line, String field, String nextField) {
    if (!StringUtils.hasText(line) || !StringUtils.hasText(field)) {
      return null;
    }
    int start = line.indexOf(field);
    if (start < 0) {
      return null;
    }
    start = start + field.length();
    int end = StringUtils.hasText(nextField)
        ? line.indexOf(nextField, start)
        : line.length();
    if (end < 0 || end <= start) {
      return null;
    }
    return line.substring(start, end).trim();
  }

  private OffsetDateTime parseDhcpLeaseTime(String time) {
    if (!StringUtils.hasText(time)) {
      return null;
    }
    final LocalDateTime localDateTime = LocalDateTime.parse(
        time,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    // As of https://linux.die.net/man/5/dhcpd.leases the time zone is always UTC
    return OffsetDateTime.of(localDateTime, ZoneOffset.UTC);
  }

}
