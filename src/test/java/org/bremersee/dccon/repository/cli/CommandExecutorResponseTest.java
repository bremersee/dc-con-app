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

import java.util.UUID;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The command executor response test.
 *
 * @author Christian Bremer
 */
@ExtendWith(SoftAssertionsExtension.class)
class CommandExecutorResponseTest {

  /**
   * Gets stdout.
   *
   * @param softly the soft assertions
   */
  @Test
  void getStdout(SoftAssertions softly) {
    String stdout = UUID.randomUUID().toString();
    CommandExecutorResponse model = new CommandExecutorResponse(stdout, null);
    softly.assertThat(model.getStdout())
        .isEqualTo(stdout);
    softly.assertThat(model.stdoutHasText())
        .isTrue();
    softly.assertThat(model.stderrHasText())
        .isFalse();
    softly.assertThat(model.toOneLine())
        .isEqualTo("stdout=[" + stdout + "]");

    String line1 = UUID.randomUUID().toString();
    String line2 = UUID.randomUUID().toString();
    model = new CommandExecutorResponse(line1 + "\n" + line2, null);
    softly.assertThat(model.toOneLine())
        .isEqualTo("stdout=[" + line1 + " | " + line2 + "]");
    softly.assertThat(model.toString())
        .contains(line1, line2);
  }

  /**
   * Gets stderr.
   *
   * @param softly the soft assertions
   */
  @Test
  void getStderr(SoftAssertions softly) {
    String stderr = UUID.randomUUID().toString();
    CommandExecutorResponse model = new CommandExecutorResponse(null, stderr);
    softly.assertThat(model.getStderr())
        .isEqualTo(stderr);
    softly.assertThat(model.stderrHasText())
        .isTrue();
    softly.assertThat(model.stdoutHasText())
        .isFalse();
    softly.assertThat(model.toOneLine())
        .isEqualTo("stderr=[" + stderr + "]");

    String line1 = UUID.randomUUID().toString();
    String line2 = UUID.randomUUID().toString();
    model = new CommandExecutorResponse(null, line1 + "\n" + line2);
    softly.assertThat(model.toOneLine())
        .isEqualTo("stderr=[" + line1 + " | " + line2 + "]");
  }

  /**
   * To one line.
   *
   * @param softly the soft assertions
   */
  @Test
  void toOneLine(SoftAssertions softly) {
    String stdout = UUID.randomUUID().toString();
    String stderr = UUID.randomUUID().toString();
    CommandExecutorResponse model = new CommandExecutorResponse(stdout, stderr);
    softly.assertThat(model.toOneLine())
        .isEqualTo("stdout=[" + stdout + "] stderr=[" + stderr + "]");

    String line1 = UUID.randomUUID().toString();
    String line2 = UUID.randomUUID().toString();
    String line3 = UUID.randomUUID().toString();
    String line4 = UUID.randomUUID().toString();
    model = new CommandExecutorResponse(line1 + "\n" + line2, line3 + "\n" + line4);
    softly.assertThat(model.toOneLine())
        .isEqualTo("stdout=[" + line1 + " | " + line2 + "] stderr=[" + line3 + " | " + line4 + "]");
  }

  /**
   * To exception message.
   *
   * @param softly the soft assertions
   */
  @Test
  void toExceptionMessage(SoftAssertions softly) {
    String stdout = UUID.randomUUID().toString();
    String stderr = UUID.randomUUID().toString();
    CommandExecutorResponse model = new CommandExecutorResponse(stdout, stderr);
    softly.assertThat(CommandExecutorResponse.toExceptionMessage(model))
        .isEqualTo("stdout=[" + stdout + "] stderr=[" + stderr + "]");
    softly.assertThat(CommandExecutorResponse.toExceptionMessage(null))
        .isEqualTo("");
  }

}