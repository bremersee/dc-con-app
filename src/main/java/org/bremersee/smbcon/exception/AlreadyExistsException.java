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

package org.bremersee.smbcon.exception;

import org.bremersee.smbcon.model.SambaGroup;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The Already exists exception is throws when an entity should be created bit is already exists.
 *
 * @author Christian Bremer
 */
@ResponseStatus(code = HttpStatus.NOT_ACCEPTABLE, reason = "Already exists.")
public class AlreadyExistsException extends RuntimeException {

  private static final long serialVersionUID = 151901075326068266L;

  /**
   * Instantiates a new Already exists exception.
   */
  @SuppressWarnings("unused")
  public AlreadyExistsException() {
    super("Already exists.");
  }

  private AlreadyExistsException(String name, boolean isUser) {
    super((isUser ? "User " : "Group [") + name + "] already exists.");
  }

  /**
   * Instantiates a new Already exists exception for the given Samba group.
   *
   * @param sambaGroup the samba group
   */
  public AlreadyExistsException(SambaGroup sambaGroup) {
    this(sambaGroup == null ? "UNSPECIFIED" : sambaGroup.getName(), false);
  }

}
