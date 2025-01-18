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

import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bremersee.dccon.model.DnsEntry;
import org.bremersee.dccon.repository.cli.CommandExecutorResponseParser;

/**
 * The interface DnsEntryParser.
 *
 * @author Christian Bremer
 */
public interface DnsEntryParser extends CommandExecutorResponseParser<Stream<DnsEntry>> {

  static DnsEntryParser defaultParser() {
    return Default.INSTANCE;
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  class Default extends AbstractCommandExecutorResponseParser<Stream<DnsEntry>> implements DnsEntryParser {

    private static final String NAME = "Name=";

    private static final String FLAGS = "(flags=";

    private static final String SERIAL = "serial=";

    private static final String TTL = "ttl=";

    private static DnsEntryParser INSTANCE;

    public static DnsEntryParser getInstance() {
      if (INSTANCE == null) {
        INSTANCE = new Default();
      }
      return INSTANCE;
    }

    @Override
    protected Stream<DnsEntry> doParse(BufferedReader reader) throws IOException {
      Stream<DnsEntry> entries = Stream.empty();
      String line;
      while ((line = reader.readLine()) != null) {
        entries = Stream.concat(entries, doParseLine(line.trim()));
      }
      return entries;
    }

    protected Stream<DnsEntry> doParseLine(String line) {
      DnsEntry entry = new DnsEntry();
      line = line.trim();
      int start = line.indexOf(':');
      if (start == -1) {
        return Stream.empty();
      }
      entry.setType(line.substring(0, start).trim());
      int end = line.indexOf(FLAGS, start + 1);
      if (end == -1) {
        end = line.length();
      }
      entry.setValue(line.substring(start + 1, end).trim());
      if (end == line.length()) {
        return Stream.of(entry);
      }
      line = line.substring(end).trim();
      entry.setFlags(findMetaData(line, FLAGS));
      String serial = findMetaData(line, SERIAL);
      if (!isEmpty(serial)) {
        try {
          entry.setSerial(Integer.parseInt(serial));
        } catch (NumberFormatException ignored) {
          // ignored
        }
      }
      String ttl = findMetaData(line, TTL);
      if (!isEmpty(ttl)) {
        try {
          entry.setTtlSeconds(Integer.parseInt(ttl));
        } catch (NumberFormatException ignored) {
          // ignored
        }
      }
      return Stream.of(entry);
    }

    private String findMetaData(String line, String name) {
      int start = line.indexOf(name);
      if (start == -1) {
        return null;
      }
      int end = line.indexOf(',', start + name.length());
      if (end == -1) {
        end = line.indexOf(')', start + name.length());
        if (end == -1) {
          end = line.length();
        }
      }
      return line.substring(start + name.length(), end).trim();
    }

  }

}
