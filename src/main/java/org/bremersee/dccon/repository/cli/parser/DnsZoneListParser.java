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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bremersee.dccon.repository.cli.CommandExecutorResponseParser;

/**
 * The interface DnsZoneListParser.
 *
 * @author Christian Bremer
 */
public interface DnsZoneListParser extends CommandExecutorResponseParser<List<String>> {

  static DnsZoneListParser defaultParser() {
    return DnsZoneListParser.Default.getInstance();
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  class Default extends AbstractCommandExecutorResponseParser<List<String>>
      implements DnsZoneListParser {

    private static DnsZoneListParser.Default INSTANCE;

    static final String ZONE_NAME = "pszZoneName";

    public static DnsZoneListParser.Default getInstance() {
      if (Objects.isNull(INSTANCE)) {
        INSTANCE = new Default();
      }
      return INSTANCE;
    }

    @Override
    protected List<String> getDefaultValue() {
      return List.of();
    }

    protected List<String> doParse(BufferedReader reader) throws IOException {
      List<String> result = new ArrayList<>();
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        int index = line.indexOf(':');
        if (line.contains(ZONE_NAME) && index > ZONE_NAME.length()) {
          String zoneName = line.substring(index + 1).trim();
          if (!zoneName.isEmpty()) {
            result.add(zoneName);
          }
        }
      }
      return result;
    }
  }

}
