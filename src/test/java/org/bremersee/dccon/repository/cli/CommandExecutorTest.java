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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.UUID;
import org.bremersee.exception.ServiceException;
import org.junit.jupiter.api.Test;

/**
 * The type Command executor test.
 *
 * @author Christian Bremer
 */
class CommandExecutorTest {

  /**
   * Exec.
   */
  @Test
  void exec() {
    assertThrows(ServiceException.class, () -> CommandExecutor
        .exec(
            Collections.singletonList(UUID.randomUUID().toString()),
            System.getProperty("java.io.tmpdir")));
  }

  /**
   * Exec with environment.
   */
  @Test
  void execWithEnvironment() {
    assertThrows(ServiceException.class, () -> CommandExecutor
        .exec(
            Collections.singletonList(UUID.randomUUID().toString()),
            Collections.emptyMap(),
            System.getProperty("java.io.tmpdir")));
  }

  /**
   * Exec with parser.
   */
  @Test
  void execWithParser() {
    assertThrows(ServiceException.class, () -> CommandExecutor
        .exec(
            Collections.singletonList(UUID.randomUUID().toString()),
            Collections.emptyMap(),
            System.getProperty("java.io.tmpdir"),
            (CommandExecutorResponseValidator) response -> {
              throw new AssertionError("Should never be called.");
            }));
  }
}