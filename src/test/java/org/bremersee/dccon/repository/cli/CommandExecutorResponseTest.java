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

package org.bremersee.dccon.repository.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * The command executor response test.
 *
 * @author Christian Bremer
 */
class CommandExecutorResponseTest {

  /**
   * Gets stdout.
   */
  @Test
  void getStdout() {
    String stdout = UUID.randomUUID().toString();
    CommandExecutorResponse model = new CommandExecutorResponse(stdout, null);
    assertEquals(stdout, model.getStdout());
    assertTrue(model.stdoutHasText());
    assertFalse(model.stderrHasText());
    assertEquals("stdout=[" + stdout + "]", model.toOneLine());

    String line1 = UUID.randomUUID().toString();
    String line2 = UUID.randomUUID().toString();
    model = new CommandExecutorResponse(line1 + "\n" + line2, null);
    assertEquals(line1 + " | " + line2, model.stdoutToOneLine());

    assertNotEquals(model, null);
    assertNotEquals(model, new Object());
    assertEquals(model, model);
    assertEquals(
        new CommandExecutorResponse("a", "b"),
        new CommandExecutorResponse("a", "b"));
    assertTrue(model.toString().contains(line1));
  }

  /**
   * Gets stderr.
   */
  @Test
  void getStderr() {
    String stderr = UUID.randomUUID().toString();
    CommandExecutorResponse model = new CommandExecutorResponse(null, stderr);
    assertEquals(stderr, model.getStderr());
    assertFalse(model.stdoutHasText());
    assertTrue(model.stderrHasText());
    assertEquals("stderr=[" + stderr + "]", model.toOneLine());

    String line1 = UUID.randomUUID().toString();
    String line2 = UUID.randomUUID().toString();
    model = new CommandExecutorResponse(null, line1 + "\n" + line2);
    assertEquals(line1 + " | " + line2, model.stderrToOneLine());
  }

  /**
   * To one line.
   */
  @Test
  void toOneLine() {
    String stdout = UUID.randomUUID().toString();
    String stderr = UUID.randomUUID().toString();
    CommandExecutorResponse model = new CommandExecutorResponse(stdout, stderr);
    assertEquals(
        "stdout=[" + stdout + "] stderr=[" + stderr + "]",
        model.toOneLine());

    String line1 = UUID.randomUUID().toString();
    String line2 = UUID.randomUUID().toString();
    String line3 = UUID.randomUUID().toString();
    String line4 = UUID.randomUUID().toString();
    model = new CommandExecutorResponse(line1 + "\n" + line2, line3 + "\n" + line4);
    assertEquals(
        "stdout=[" + line1 + " | " + line2 + "] stderr=[" + line3 + " | " + line4 + "]",
        model.toOneLine());
  }

  /**
   * To exception message.
   */
  @Test
  void toExceptionMessage() {
    String stdout = UUID.randomUUID().toString();
    String stderr = UUID.randomUUID().toString();
    CommandExecutorResponse model = new CommandExecutorResponse(stdout, stderr);
    assertEquals(
        "stdout=[" + stdout + "] stderr=[" + stderr + "]",
        CommandExecutorResponse.toExceptionMessage(model));
    assertEquals("", CommandExecutorResponse.toExceptionMessage(null));
  }

}