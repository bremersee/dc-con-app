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
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.model.DnsEntry;
import org.bremersee.dccon.model.DnsEntryType;
import org.bremersee.dccon.repository.cli.CommandExecutorResponseParser;
import org.bremersee.dccon.repository.mapper.CommonAttributesLdapMapper;
import org.bremersee.ldaptive.LdaptiveTemplate;
import org.ldaptive.dn.Dn;
import org.ldaptive.dn.NameValue;
import org.ldaptive.dn.RDn;

/**
 * The interface DnsEntriesParser.
 *
 * @author Christian Bremer
 */
public interface DnsEntriesParser extends
    CommandExecutorResponseParser<Stream<DnsEntry>> {

  static DnsEntriesParser defaultParser(
      LdaptiveTemplate ldaptiveTemplate, Dn zoneDn, String name) {
    return new Default(ldaptiveTemplate, zoneDn, name);
  }

  @Slf4j
  class Default extends AbstractCommandExecutorResponseParser<Stream<DnsEntry>>
      implements DnsEntriesParser {

    private static final String NAME = "Name=";

    private static final String RECORDS = ", Records=";

    private static final String CONFLICT = "CNF";

    private static final char RECORD_LINE_INDICATOR = ':';

    private static final String FLAGS = "(flags=";

    private static final String SERIAL = ", serial=";

    private static final String TTL = ", ttl=";

    private static final char END = ')';

    private final LdaptiveTemplate ldaptiveTemplate;

    private final Dn zoneDn;

    private final String name;

    Default(LdaptiveTemplate ldaptiveTemplate, Dn zoneDn, String name) {
      this.ldaptiveTemplate = ldaptiveTemplate;
      this.zoneDn = zoneDn;
      this.name = name;
    }

    @Override
    protected Stream<DnsEntry> doParse(BufferedReader reader) throws IOException {
      Stream<DnsEntry> entries = Stream.empty();
      DnsEntry currentEntry = null;
      String line;
      while (nonNull(line = reader.readLine())) {
        line = line.trim();
        if (line.startsWith(NAME)) {
          int end = line.indexOf(RECORDS);
          String name;
          if (end > 0) {
            name = line.substring(NAME.length(), end).trim();
          } else {
            name = line.substring(NAME.length()).trim();
          }
          if (name.isEmpty()) {
            name = this.name;
          }
          currentEntry = new DnsEntry(name);
        } else if (nonNull(currentEntry)) {
          int i0 = line.indexOf(RECORD_LINE_INDICATOR);
          if (i0 > 0) {
            String recordType = line.substring(0, i0).trim();
            if (CONFLICT.equalsIgnoreCase(recordType)) {
              currentEntry.setConflict(true);
              int i1 = line.indexOf(RECORDS);
              if (i1 > i0) {
                currentEntry.setObjectGuid(line.substring(i0 + 1, i1).trim());
              } else {
                currentEntry.setObjectGuid(line.substring(i0 + 1).trim());
              }
            } else {
              parseDnsRecord(line, currentEntry);
              String name = currentEntry.getName();
              if (!isEmpty(name) && !isEmpty(currentEntry.getType())
                  && !DnsEntryType.ALL.equals(currentEntry.getType())
                  && !isEmpty(currentEntry.getValue())) {
                setCommonAttributes(currentEntry);
                entries = Stream.concat(entries, Stream.of(currentEntry));
                currentEntry = new DnsEntry(currentEntry.getName());
              }
            }
          }
        }
      }
      return entries;
    }

    private void parseDnsRecord(String line, DnsEntry currentEntry) {
      int i0 = line.indexOf(RECORD_LINE_INDICATOR);
      if (i0 > 0) {
        currentEntry.setType(DnsEntryType.fromValue(line.substring(0, i0).trim(), null));
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
            if (i1 > i0) {
              String serial = line.substring(i0, i1).trim();
              try {
                currentEntry.setSerial(Integer.parseInt(serial));
              } catch (NumberFormatException ignored) {
                // ignored
              }
              i0 = i1 + TTL.length();
              i1 = line.indexOf(END, i0);
              if (i1 > i0) {
                String ttl = line.substring(i0, i1).trim();
                try {
                  currentEntry.setTtlSeconds(Integer.parseInt(ttl));
                } catch (NumberFormatException ignored) {
                  // ignored
                }
              }
            }
          }
        }
      }
    }

    private void setCommonAttributes(DnsEntry currentEntry) {
      if (!isEmpty(zoneDn) && !zoneDn.isEmpty()) {
        Dn dn = new Dn(new RDn(new NameValue("DC", currentEntry.getName())));
        dn.add(zoneDn);
        currentEntry.setDn(dn);
        CommonAttributesLdapMapper.mapCommonAttributes(ldaptiveTemplate, dn,
            currentEntry);
      }
    }

  }

}
