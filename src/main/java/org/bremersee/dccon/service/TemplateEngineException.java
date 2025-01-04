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

package org.bremersee.dccon.service;

import lombok.Getter;
import org.bremersee.dccon.ErrorCode;
import org.bremersee.exception.ErrorCodeAware;
import org.bremersee.exception.HttpStatusAware;
import org.springframework.http.HttpStatus;

/**
 * The type TemplateEngineException.
 *
 * @author Christian Bremer
 */
@Getter
public class TemplateEngineException extends IllegalArgumentException
    implements ErrorCodeAware, HttpStatusAware {

  /**
   * The key that caused the problem.
   */
  public final String key;

  /**
   * The line number of the template on which the problem occurred.
   */
  public final int lineNo;

  public TemplateEngineException(String message, String key, int lineNo, Throwable cause) {
    super(message, cause);
    this.key = key;
    this.lineNo = lineNo;
  }

  @Override
  public String getErrorCode() {
    return ErrorCode.EC_TEMPLATE_WITH_MISSING_KEY;
  }

  @Override
  public int status() {
    return HttpStatus.BAD_REQUEST.value();
  }
}
