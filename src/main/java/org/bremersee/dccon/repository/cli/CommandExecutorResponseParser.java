/*
 * Copyright 2019-2020 the original author or authors.
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

/**
 * The command executor response parser.
 *
 * @param <T> the type of the parsed response
 * @author Christian Bremer
 */
public interface CommandExecutorResponseParser<T> {

  /**
   * Parses the response of the command executor.
   *
   * @param response the response of the command executor
   * @return the type of the parsed response
   */
  T parse(CommandExecutorResponse response);

}
