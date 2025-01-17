/*
 * Copyright 2025 the original author or authors.
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

package org.bremersee.dccon.repository.cli.parser;

import static java.util.Objects.isNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.repository.cli.CommandExecutorResponseParser;

/**
 * The interface DnsZoneListParser.
 *
 * @author Christian Bremer
 */
public interface DnsZoneParser extends CommandExecutorResponseParser<DnsZone> {

  static DnsZoneParser defaultParser() {
    return DnsZoneParser.Default.getInstance();
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  class Default extends AbstractCommandExecutorResponseParser<DnsZone>
      implements DnsZoneParser {

    private static final String ZONE_NAME = "pszZoneName";

    private static final String ZONE_TYPE = "dwZoneType";

    private static final String IS_REVERSE = "fReverse";

    private static final String ALLOW_UPDATE = "fAllowUpdate";

    private static final String IS_PAUSED = "fPaused";

    private static final String IS_SHUTDOWN = "fShutdown";

    private static final String IS_AUTO_CREATED = "fAutoCreated";

    private static final String IS_USING_DATABASE = "fUseDatabase";

    private static final String DATA_FILE = "pszDataFile";

    private static final String IS_USING_WINS = "fUseWins";

    private static final String IS_USING_NBSTAT = "fUseNbstat";

    private static final String IS_AGING = "fAging";

    private static final String FQDN = "pszDpFqdn";

    private static final String ZONE_DN = "pwszZoneDn";

    private static final String IS_QUEUED_FOR_BACKGROUND_LOAD = "fQueuedForBackgroundLoad";

    private static final String IS_BACKGROUND_LOAD_IN_PRPGRESS = "fBackgroundLoadInProgress";

    private static final String IS_READ_ONLY_ZONE = "fReadOnlyZone";

    private static DnsZoneParser.Default INSTANCE;

    public static DnsZoneParser getInstance() {
      if (isNull(INSTANCE)) {
        INSTANCE = new DnsZoneParser.Default();
      }
      return INSTANCE;
    }

    protected DnsZone doParse(BufferedReader reader) throws IOException {
      DnsZone zone = new DnsZone();
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        int index = line.indexOf(':');
        if (lineContains(line, ZONE_NAME, index)) {
          zone.setName(line.substring(index + 1).trim());
        }
        if (lineContains(line, ZONE_TYPE, index)) {
          zone.setZoneType(line.substring(index + 1).trim());
        }
        if (lineContains(line, IS_REVERSE, index)) {
          String value = line.substring(index + 1).trim();
          zone.setReverseZone(Boolean.parseBoolean(value));
        }
        if (lineContains(line, ALLOW_UPDATE, index)) {
          zone.setZoneType(line.substring(index + 1).trim());
        }
        if (lineContains(line, IS_PAUSED, index)) {
          String value = line.substring(index + 1).trim();
          zone.setPaused(Boolean.parseBoolean(value));
        }
        if (lineContains(line, IS_SHUTDOWN, index)) {
          String value = line.substring(index + 1).trim();
          zone.setShutdown(Boolean.parseBoolean(value));
        }
        if (lineContains(line, IS_AUTO_CREATED, index)) {
          String value = line.substring(index + 1).trim();
          zone.setAutoCreated(Boolean.parseBoolean(value));
        }
        if (lineContains(line, IS_USING_DATABASE, index)) {
          String value = line.substring(index + 1).trim();
          zone.setUseDatabase(Boolean.parseBoolean(value));
        }
        if (lineContains(line, DATA_FILE, index)) {
          zone.setDataFile(line.substring(index + 1).trim());
        }
        if (lineContains(line, IS_USING_WINS, index)) {
          String value = line.substring(index + 1).trim();
          zone.setUseWins(Boolean.parseBoolean(value));
        }
        if (lineContains(line, IS_USING_NBSTAT, index)) {
          String value = line.substring(index + 1).trim();
          zone.setUseNbstat(Boolean.parseBoolean(value));
        }
        if (lineContains(line, IS_AGING, index)) {
          String value = line.substring(index + 1).trim();
          zone.setAging(Boolean.parseBoolean(value));
        }
        if (lineContains(line, FQDN, index)) {
          zone.setFqdn(line.substring(index + 1).trim());
        }
        if (lineContains(line, ZONE_DN, index)) {
          zone.setDistinguishedName(line.substring(index + 1).trim());
        }
        if (lineContains(line, IS_QUEUED_FOR_BACKGROUND_LOAD, index)) {
          String value = line.substring(index + 1).trim();
          zone.setQueuedForBackgroundLoad(Boolean.parseBoolean(value));
        }
        if (lineContains(line, IS_BACKGROUND_LOAD_IN_PRPGRESS, index)) {
          String value = line.substring(index + 1).trim();
          zone.setBackgroundLoadInProgress(Boolean.parseBoolean(value));
        }
        if (lineContains(line, IS_READ_ONLY_ZONE, index)) {
          String value = line.substring(index + 1).trim();
          zone.setReadOnlyZone(Boolean.parseBoolean(value));
        }
      }
      return Objects.nonNull(zone.getName()) ? zone : null;
    }

    private static boolean lineContains(String line, String name, int index) {
      return line.contains(name) && index > line.length();
    }

  }

}
