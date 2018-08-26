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

package org.bremersee.smbcon.business;

import java.io.File;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.bremersee.common.exhandling.ServiceException;
import org.springframework.http.HttpStatus;

/**
 * The interface Command executor.
 *
 * @author Christian Bremer
 */
public interface CommandExecutor {

  /**
   * Exec command executor response.
   *
   * @param commands the commands
   * @param dir      the dir
   * @return the command executor response
   */
  static CommandExecutorResponse exec(
      final List<String> commands,
      final String dir) {

    return exec(commands, null, dir);
  }

  /**
   * Exec command executor response.
   *
   * @param commands the commands
   * @param env      the env
   * @param dir      the dir
   * @return the command executor response
   */
  static CommandExecutorResponse exec(
      final List<String> commands,
      final Map<String, String> env,
      final String dir) {

    try {
      ProcessBuilder pb = new ProcessBuilder(commands);
      if (dir != null && dir.trim().length() > 0) {
        pb.directory(new File(dir));
      }
      if (env != null && !env.isEmpty()) {
        pb.environment().putAll(env);
      }

      final Process p = pb.start();
      final StringWriter out = new StringWriter();
      final StringWriter err = new StringWriter();
      IOUtils.copy(p.getInputStream(), out, StandardCharsets.UTF_8);
      IOUtils.copy(p.getErrorStream(), err, StandardCharsets.UTF_8);
      p.waitFor();
      return new CommandExecutorResponse(out.toString(), err.toString());

    } catch (Exception e) {
      throw new ServiceException(
          HttpStatus.INTERNAL_SERVER_ERROR.value(),
          "Running commands " + commands + " failed.",
          e);
    }
  }
}
