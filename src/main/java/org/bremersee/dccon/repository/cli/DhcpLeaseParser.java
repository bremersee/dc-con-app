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

package org.bremersee.dccon.repository.cli;

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
import java.util.function.BiFunction;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.model.DhcpLease;
import org.springframework.util.StringUtils;

/**
 * The dhcp lease list parser parses the response of the linux command line tool {@code
 * dhcp-lease-list}.
 *
 * <p>A response of {@code dhcp-lease-list} looks like this:
 * <pre>
 * MAC b8:xx:xx:xx:xx:xx IP 192.168.1.109 HOSTNAME ukelei BEGIN 2019-08-18 11:20:33 END 2019-08-18 11:50:33 MANUFACTURER Apple, Inc.
 * MAC ac:xx:xx:xx:xx:yy IP 192.168.1.188 HOSTNAME -NA- BEGIN 2019-08-18 11:25:48 END 2019-08-18 11:55:48 MANUFACTURER Super Micro Computer, Inc.
 * </pre>
 *
 * @author Christian Bremer
 */
public interface DhcpLeaseParser extends CommandExecutorResponseParser<List<DhcpLease>> {

  /**
   * The constant MAC.
   */
  String MAC = "MAC ";

  /**
   * The constant IP.
   */
  String IP = " IP ";

  /**
   * The constant HOSTNAME.
   */
  String HOSTNAME = " HOSTNAME ";

  /**
   * The constant HOSTNAME_UNKNOWN.
   */
  String HOSTNAME_UNKNOWN = "-NA-";

  /**
   * The constant BEGIN.
   */
  String BEGIN = " BEGIN ";

  /**
   * The constant END.
   */
  String END = " END ";

  /**
   * The constant MANUFACTURER.
   */
  String MANUFACTURER = " MANUFACTURER ";

  /**
   * Default parser dhcp leases parser.
   *
   * @return the dhcp leases parser
   */
  static DhcpLeaseParser defaultParser() {
    return new Default();
  }

  /**
   * Default parser dhcp leases parser.
   *
   * @param unknownHostConverter the unknown host converter
   * @return the dhcp leases parser
   */
  @SuppressWarnings("unused")
  static DhcpLeaseParser defaultParser(BiFunction<String, String, String> unknownHostConverter) {
    return new Default(unknownHostConverter);
  }

  /**
   * The default parser.
   */
  @Slf4j
  class Default implements DhcpLeaseParser {

    private BiFunction<String, String, String> unknownHostConverter;

    /**
     * Instantiates a new default parser.
     */
    Default() {
      this(null);
    }

    /**
     * Instantiates a new default parser.
     *
     * <p>The unknown host converter converts the unknown host name {@link
     * DhcpLeaseParser#HOSTNAME_UNKNOWN} into another host name. The parameters of the function are
     * MAC and IP.
     *
     * @param unknownHostConverter the unknown host converter
     */
    Default(
        BiFunction<String, String, String> unknownHostConverter) {
      if (unknownHostConverter == null) {
        this.unknownHostConverter = (mac, ip) -> "dhcp-" + ip.replace(".", "-");
      } else {
        this.unknownHostConverter = unknownHostConverter;
      }
    }

    @Override
    public List<DhcpLease> parse(final CommandExecutorResponse response) {
      final String output = response.getStdout();
      if (!StringUtils.hasText(output)) {
        log.warn("Dhcp lease list command did not produce output. Error is [{}].",
            response.getStderr());
        return Collections.emptyList();
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
        String hostname = findDhcpLeasePart(line, HOSTNAME, BEGIN);
        if (HOSTNAME_UNKNOWN.equalsIgnoreCase(hostname) && ip != null) {
          hostname = unknownHostConverter.apply(mac, ip);
        }
        final String begin = findDhcpLeasePart(line, BEGIN, END);
        final String end = findDhcpLeasePart(line, END, MANUFACTURER);
        final String manufacturer = findDhcpLeasePart(line, MANUFACTURER, null);
        if (mac != null && ip != null && hostname != null && begin != null && end != null) {
          final DhcpLease lease = new DhcpLease(
              mac.replace("-", ":").trim().toLowerCase(),
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

}
