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
import java.io.StringReader;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.repository.cli.CommandExecutorResponse;
import org.bremersee.dccon.repository.cli.CommandExecutorResponseParser;

/**
 * The type AbstractCommandExecutorResponseParser.
 *
 * @author Christian Bremer
 */
@Slf4j
public abstract class AbstractCommandExecutorResponseParser<T>
    implements CommandExecutorResponseParser<T> {

  protected abstract T getDefaultValue();

  @Override
  public T parse(CommandExecutorResponse response) {
    if (!response.stdoutHasText()) {
      if (response.stderrHasText()) {
        log.warn("Command did not produce output. Error is:\n{}\n",
            response.getStderr());
      } else {
        log.warn("Command did not produce output. Error is also not present.");
      }
      return getDefaultValue();
    }
    String output = response.getStdout();
    try (BufferedReader reader = new BufferedReader(new StringReader(output))) {
      return doParse(reader);

    } catch (IOException e) {
      log.error("Parsing response of command failed:\n{}\n", output, e);
      return getDefaultValue();
    }
  }

  protected abstract T doParse(BufferedReader reader) throws IOException;
}
