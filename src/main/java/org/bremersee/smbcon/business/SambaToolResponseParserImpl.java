/*
 * Copyright 2017 the original author or authors.
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

package org.bremersee.smbcon.business;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.smbcon.model.DnsDwDpZoneFlag;
import org.bremersee.smbcon.model.DnsEntry;
import org.bremersee.smbcon.model.DnsRecord;
import org.bremersee.smbcon.model.DnsZone;
import org.bremersee.smbcon.model.DnsZoneFlag;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author Christian Bremer
 */
@Component
@Slf4j
public class SambaToolResponseParserImpl implements SambaToolResponseParser {

  private static final String PSZ_ZONE_NAME = "pszZoneName";

  private static final String FLAGS = "flags";

  private static final String ZONE_TYPE = "ZoneType";

  private static final String VERSION = "Version";

  private static final String DW_DP_FLAGS = "dwDpFlags";

  private static final String PSZ_DP_FQDN = "pszDpFqdn";

  private static final String DNS_ENTRY_NAME = "Name=";

//  3 zone(s) found
//
//  pszZoneName                 : 1.168.192.in-addr.arpa
//  Flags                       : DNS_RPC_ZONE_DSINTEGRATED DNS_RPC_ZONE_UPDATE_SECURE
//  ZoneType                    : DNS_ZONE_TYPE_PRIMARY
//  Version                     : 50
//  dwDpFlags                   : DNS_DP_AUTOCREATED DNS_DP_DOMAIN_DEFAULT DNS_DP_ENLISTED
//  pszDpFqdn                   : DomainDnsZones.eixe.bremersee.org
//
//  pszZoneName                 : eixe.bremersee.org
//  Flags                       : DNS_RPC_ZONE_DSINTEGRATED DNS_RPC_ZONE_UPDATE_SECURE
//  ZoneType                    : DNS_ZONE_TYPE_PRIMARY
//  Version                     : 50
//  dwDpFlags                   : DNS_DP_AUTOCREATED DNS_DP_DOMAIN_DEFAULT DNS_DP_ENLISTED
//  pszDpFqdn                   : DomainDnsZones.eixe.bremersee.org
//
//  pszZoneName                 : _msdcs.eixe.bremersee.org
//  Flags                       : DNS_RPC_ZONE_DSINTEGRATED DNS_RPC_ZONE_UPDATE_SECURE
//  ZoneType                    : DNS_ZONE_TYPE_PRIMARY
//  Version                     : 50
//  dwDpFlags                   : DNS_DP_AUTOCREATED DNS_DP_FOREST_DEFAULT DNS_DP_ENLISTED
//  pszDpFqdn                   : ForestDnsZones.eixe.bremersee.org

  @Override
  public List<DnsZone> parseDnsZones(@NotNull final CommandExecutorResponse response) {

    final String output = response.getOutput();
    if (!StringUtils.hasText(output)) {

      log.error("Dns zone list command did not produce output. Error is [{}].",
          response.getError());
      return Collections.emptyList();

    } else {

      if (log.isDebugEnabled()) {
        log.debug("Parsing zone list:\n{}", output);
      }
      try (final BufferedReader reader = new BufferedReader(new StringReader(output))) {
        return parseDnsZones(reader);

      } catch (IOException e) {
        log.error("Parsing zone list failed:\n" + output + "\n", e);
        return Collections.emptyList();
      }
    }
  }

  private List<DnsZone> parseDnsZones( // NOSONAR
      @NotNull final BufferedReader reader) throws IOException {

    final List<DnsZone> zoneList = new ArrayList<>();
    DnsZone zone = null;
    String line;
    while ((line = reader.readLine()) != null) {
      line = line.trim();
      if (line.startsWith(PSZ_ZONE_NAME) && line.indexOf(':', PSZ_ZONE_NAME.length()) > 0) {
        line = line.substring(line.indexOf(':', PSZ_ZONE_NAME.length()) + 1).trim();
        if (StringUtils.hasText(line)) {
          if (log.isDebugEnabled()) {
            log.debug("Dns zone found: {}", line);
          }
          zone = new DnsZone();
          zone.setPszZoneName(line);
          zoneList.add(zone);
        }

      } else if (line.startsWith(FLAGS) && zone != null
          && line.indexOf(':', FLAGS.length()) > 0) {
        line = line.substring(line.indexOf(':', FLAGS.length()) + 1).trim();
        if (StringUtils.hasText(line)) {
          final String[] parts = StringUtils.delimitedListToStringArray(line, " ");
          if (parts.length > 0) {
            zone.setFlags(
                Arrays.stream(parts)
                    .map(new DnsZoneFlag()::name)
                    .collect(Collectors.toList()));
          }
        }

      } else if (line.startsWith(ZONE_TYPE) && zone != null
          && line.indexOf(':', ZONE_TYPE.length()) > 0) {
        line = line.substring(line.indexOf(':', ZONE_TYPE.length()) + 1).trim();
        if (StringUtils.hasText(line)) {
          zone.setZoneType(line);
        }

      } else if (line.startsWith(VERSION) && zone != null
          && line.indexOf(':', VERSION.length()) > 0) {
        line = line.substring(line.indexOf(':', VERSION.length()) + 1).trim();
        if (StringUtils.hasText(line)) {
          zone.setVersion(line);
        }

      } else if (line.startsWith(DW_DP_FLAGS) && zone != null
          && line.indexOf(':', DW_DP_FLAGS.length()) > 0) {
        line = line.substring(line.indexOf(':', DW_DP_FLAGS.length()) + 1).trim();
        if (StringUtils.hasText(line)) {
          final String[] parts = StringUtils.delimitedListToStringArray(line, " ");
          if (parts.length > 0) {
            zone.setDwDpFlags(
                Arrays.stream(parts)
                    .map(new DnsDwDpZoneFlag()::name)
                    .collect(Collectors.toList()));
          }
        }

      } else if (line.startsWith(PSZ_DP_FQDN) && zone != null
          && line.indexOf(':', PSZ_DP_FQDN.length()) > 0) {
        line = line.substring(line.indexOf(':', PSZ_DP_FQDN.length()) + 1).trim();
        if (StringUtils.hasText(line)) {
          zone.setPszDpFqdn(line);
        }

      }
    }
    return zoneList;
  }

  @Override
  public List<DnsEntry> parseDnsRecords(@NotNull CommandExecutorResponse response) {
    final String output = response.getOutput();
    if (!StringUtils.hasText(output)) {

      log.error("Dns records command did not produce output. Error is [{}].",
          response.getError());
      return Collections.emptyList();

    } else {

      if (log.isDebugEnabled()) {
        log.debug("Parsing nameserver records:\n{}", output);
      }
      try (final BufferedReader reader = new BufferedReader(new StringReader(output))) {
        return parseDnsRecords(reader);

      } catch (IOException e) {
        log.error("Parsing nameserver records failed:\n" + output + "\n", e);
        return Collections.emptyList();
      }
    }
  }

  private List<DnsEntry> parseDnsRecords( // NOSONAR
      @NotNull final BufferedReader reader) throws IOException {

    log.debug("Parsing dns records ...");
    final List<DnsEntry> entryList = new ArrayList<>();
    DnsEntry dnsEntry = null;
    String line;
    while ((line = reader.readLine()) != null) {
      line = line.trim();
      log.debug("Line: {}", line);
      final int nameSeparator = line.indexOf(',', DNS_ENTRY_NAME.length());
      final int recordSeparator = line.indexOf(':');
      final int infoStart = line.lastIndexOf('(');
      final int infoEnd = line.lastIndexOf(')');
      log.debug("Indexes: nameSeparator={} recordSeparator={} infoStart={} infoEnd={}",
          nameSeparator, recordSeparator, infoStart, infoEnd);
      if (line.startsWith(DNS_ENTRY_NAME) && nameSeparator > 0) {
        log.debug("Line with entry name.");
        line = line.substring(DNS_ENTRY_NAME.length(), nameSeparator);
        dnsEntry = new DnsEntry().name(line);
        entryList.add(dnsEntry);
      } else if (dnsEntry != null && recordSeparator > 0 && recordSeparator < infoStart
          && infoStart < infoEnd) {
        log.debug("Line with record.");
        final String recordType = line.substring(0, recordSeparator).trim();
        final String recordValue = line.substring(recordSeparator + 1, infoStart).trim();
        if (StringUtils.hasText(recordType) && StringUtils.hasText(recordValue)) {
          final DnsRecord dnsRecord = new DnsRecord()
              .recordType(recordType)
              .recordValue(recordValue);
          dnsEntry.addRecordsItem(dnsRecord);
          final String[] infoPairs = StringUtils.delimitedListToStringArray(
              line.substring(infoStart + 1, infoEnd),
              ", ");
          for (String infoPair : infoPairs) {
            final String[] pair = StringUtils.delimitedListToStringArray(infoPair, "=");
            if (pair.length >= 2 && pair[0].trim().equals(FLAGS)
                && StringUtils.hasText(pair[1].trim())) {
              dnsRecord.setFlags(pair[1].trim());
            } else if (pair.length >= 2 && pair[0].trim().equals("serial")
                && StringUtils.hasText(pair[1].trim())) {
              dnsRecord.setSerial(pair[1].trim());
            } else if (pair.length >= 2 && pair[0].trim().equals("ttl")
                && StringUtils.hasText(pair[1].trim())) {
              dnsRecord.setTtl(pair[1].trim());
            }

          }
        }
      } else {
        log.debug("No dns record relevant line.");
      }
    }
    return entryList;
  }

}
