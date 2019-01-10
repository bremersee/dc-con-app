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

/**
 * The not found exception is throws when an entity could not be found.
 *
 * @author Christian Bremer
 */
public class NotFoundException extends RuntimeException {

  private static final long serialVersionUID = 2235473063263765047L;

  /**
   * Instantiates a new not found exception.
   */
  NotFoundException() {
    super("Not found.");
  }

  /**
   * Instantiates a new not found exception.
   *
   * @param message the message
   */
  NotFoundException(String message) {
    super(message);
  }
}
