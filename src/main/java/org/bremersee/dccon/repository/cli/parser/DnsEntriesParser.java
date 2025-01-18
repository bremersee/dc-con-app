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

import static java.util.Objects.nonNull;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bremersee.dccon.model.DnsEntry;
import org.bremersee.dccon.repository.cli.CommandExecutorResponseParser;

/**
 * The interface DnsEntriesParser.
 *
 * @author Christian Bremer
 */
public interface DnsEntriesParser extends CommandExecutorResponseParser<List<DnsEntry>> {

  // TODO needs properties and ldaptive template to resolve conflicts

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  class Default extends AbstractCommandExecutorResponseParser<List<DnsEntry>> implements
      DnsEntriesParser {

    private static final String NAME = "Name=";

    private static final String RECORDS = ", Records=";

    private static final char VALUE_LINE_INDICATOR = ':';

    private static final String CONFLICT = "CNF";

    private static final String FLAGS = "(flags=";

    private static final String SERIAL = ", serial=";

    private static final String TTL = ", ttl=";

    private static final char END = ')';

    @Override
    protected List<DnsEntry> doParse(BufferedReader reader) throws IOException {
      List<DnsEntry> entries = new ArrayList<>();
      DnsEntry currentEntry = null;
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.startsWith(NAME)) {
          int end = line.indexOf(RECORDS);
          String name = line.substring(NAME.length(), end).trim();
          if (name.isEmpty()) {
            name = "@"; // TODO create class DnsZoneEntries mit separatem @ eintrag
          }
          currentEntry = new DnsEntry();
          currentEntry.setName(name);
        } else if (nonNull(currentEntry)) {
          int i0 = line.indexOf(VALUE_LINE_INDICATOR);
          if (i0 > 0) {
            String recordType = line.substring(0, i0).trim();
            if (CONFLICT.equalsIgnoreCase(recordType)) {
              // TODO
            } else {
              currentEntry.setType(recordType);
              int i1 = line.indexOf(FLAGS, i0 + 1);
              if (i1 > i0) {
                String value = line.substring(i0 + 1, i1).trim();
                currentEntry.setValue(value);
                i0 = i1 + FLAGS.length();
                i1 = line.indexOf(SERIAL, i0);
                if (i1 > i0) {
                  String flags = line.substring(i0, i1).trim();
                  currentEntry.setFlags(flags);
                  i0 = i1 + SERIAL.length();
                  i1 = line.indexOf(TTL, i0);
                  String serial = line.substring(i0, i1).trim();
                  try {
                    currentEntry.setSerial(Integer.parseInt(serial));
                  } catch (NumberFormatException ignored) {
                    // ignored
                  }
                  i0 = i1 + TTL.length();
                  i1 = line.indexOf(END, i0);
                  String ttl = line.substring(i0, i1).trim();
                  try {
                    currentEntry.setTtlSeconds(Integer.parseInt(ttl));
                  } catch (NumberFormatException ignored) {
                    // ignored
                  }
                }
                if (!isEmpty(currentEntry.getType()) && !isEmpty(currentEntry.getValue())) {
                  entries.add(currentEntry);
                }
                currentEntry = new DnsEntry(currentEntry.getName());
              }
            }
          }
        }
      }
      /*
      if (!isEmpty(currentEntry)
          && !isEmpty(currentEntry.getType()) && !isEmpty(currentEntry.getValue())) {
        entries.add(currentEntry);
      }
      */
      return entries;
    }

  }

}
