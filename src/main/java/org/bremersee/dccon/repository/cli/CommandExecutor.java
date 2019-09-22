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

import java.io.File;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.bremersee.exception.ServiceException;
import org.springframework.http.HttpStatus;

/**
 * The command executor.
 *
 * @author Christian Bremer
 */
@Slf4j
public abstract class CommandExecutor {

  private CommandExecutor() {
  }

  /**
   * Exec command executor.
   *
   * @param commands the commands
   * @param dir      the dir
   * @return the command executor response
   */
  @SuppressWarnings("UnusedReturnValue")
  public static CommandExecutorResponse exec(
      final List<String> commands,
      final String dir) {

    return exec(commands, null, dir);
  }

  /**
   * Exec command executor.
   *
   * @param commands the commands
   * @param env      the env
   * @param dir      the dir
   * @return the command executor response
   */
  @SuppressWarnings("WeakerAccess")
  public static CommandExecutorResponse exec(
      final List<String> commands,
      final Map<String, String> env,
      final String dir) {

    return exec(commands, env, dir, response -> response);
  }

  /**
   * Exec command executor.
   *
   * @param <T>            the type parameter
   * @param commands       the commands
   * @param env            the env
   * @param dir            the dir
   * @param responseParser the response parser
   * @return the command executor response
   */
  public static <T> T exec(
      final List<String> commands,
      final Map<String, String> env,
      final String dir,
      final CommandExecutorResponseParser<T> responseParser) {

    try {
      ProcessBuilder pb = new ProcessBuilder(commands);
      if (dir != null && dir.trim().length() > 0) {
        pb.directory(new File(dir));
      }
      if (env != null && !env.isEmpty()) {
        pb.environment().putAll(env);
      }

      if (log.isDebugEnabled()) {
        log.debug("msg=[Running external program.] commands=[{}]", commands);
      }
      final Process p = pb.start();
      final StringWriter out = new StringWriter();
      final StringWriter err = new StringWriter();
      IOUtils.copy(p.getInputStream(), out, StandardCharsets.UTF_8);
      IOUtils.copy(p.getErrorStream(), err, StandardCharsets.UTF_8);
      p.waitFor();
      final String output = out.toString();
      final String error = err.toString();
      if (log.isDebugEnabled()) {
        log.debug("msg=[Program output]\n{}", output);
        log.debug("msg=[Program error output]\n{}", error);
      }
      return responseParser.parse(new CommandExecutorResponse(output, error));

    } catch (final Exception e) {
      throw new ServiceException(
          HttpStatus.INTERNAL_SERVER_ERROR.value(),
          "Running commands " + commands + " failed.",
          "org.bremersee:dc-con-app:6fa0f473-6204-4f75-9130-a1049910d8fd",
          e);
    }
  }

}
