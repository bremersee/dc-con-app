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

import java.io.BufferedReader;
import java.io.IOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bremersee.dccon.repository.cli.CommandExecutorResponseParser;

/**
 * The interface HostNameResponseParser.
 *
 * @author Christian Bremer
 */
public interface HostNameResponseParser extends CommandExecutorResponseParser<String> {

  static HostNameResponseParser defaultParser() {
    return Default.getInstance();
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  class Default extends AbstractCommandExecutorResponseParser<String>
      implements HostNameResponseParser {

    private static HostNameResponseParser INSTANCE;

    public static HostNameResponseParser getInstance() {
      if (INSTANCE == null) {
        INSTANCE = new Default();
      }
      return INSTANCE;
    }

    @Override
    protected String getDefaultValue() {
      return "";
    }

    @Override
    protected String doParse(BufferedReader reader) throws IOException {
      String hostName = reader.readLine();
      if (nonNull(hostName)) {
        return hostName.trim();
      }
      return null;
    }
  }

}
