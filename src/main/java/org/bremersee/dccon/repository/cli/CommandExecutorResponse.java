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

import java.io.Serializable;
import java.util.StringTokenizer;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.StringUtils;

/**
 * The command executor response.
 *
 * @author Christian Bremer
 */
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class CommandExecutorResponse implements Serializable {

  private final String stdout;

  private final String stderr;

  /**
   * Check whether stdout has text or not.
   *
   * @return {@code true} if stdout has text, otherwise {@code false}
   */
  public boolean stdoutHasText() {
    return hasText(stdout);
  }

  /**
   * Check whether stderr has text or not.
   *
   * @return {@code true} if stderr has text, otherwise {@code false}
   */
  public boolean stderrHasText() {
    return hasText(stderr);
  }

  private boolean hasText(String value) {
    return value != null && value.trim().length() > 0;
  }

  /**
   * Returns stdout and stderr in one line.
   *
   * @return the line
   */
  public String toOneLine() {
    final String out = stdoutToOneLine();
    final String err = stderrToOneLine();
    final StringBuilder sb = new StringBuilder();
    if (StringUtils.hasText(out)) {
      sb.append("stdout=[").append(out).append("]");
      if (StringUtils.hasText(err)) {
        sb.append(" ");
      }
    }
    if (StringUtils.hasText(err)) {
      sb.append("stderr=[").append(err).append("]");
    }
    return sb.toString();
  }

  private String toOneLine(String value) {

    if (!hasText(value)) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    StringTokenizer tokenizer = new StringTokenizer(value.trim(), "\n\r\f");
    int n = 0;
    while (tokenizer.hasMoreTokens()) {
      if (n > 0) {
        sb.append(" | ");
      }
      sb.append(tokenizer.nextToken().trim());
      n++;
    }
    return sb.toString();
  }

  /**
   * Stdout to one line.
   *
   * @return the line
   */
  public String stdoutToOneLine() {
    return toOneLine(stdout);
  }

  /**
   * Stderr to one line.
   *
   * @return the line
   */
  public String stderrToOneLine() {
    return toOneLine(stderr);
  }

  /**
   * Return {@link #toOneLine()} if response is not {@code null}.
   *
   * @param response the response (can be {@code null})
   * @return the response in one line
   */
  public static String toExceptionMessage(CommandExecutorResponse response) {
    return response == null ? "" : response.toOneLine();
  }

}
