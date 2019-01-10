/*
 * Copyright 2018 the original author or authors.
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

import org.bremersee.exception.annotation.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The type Group already exists exception.
 *
 * @author Christian Bremer
 */
@ResponseStatus(code = HttpStatus.NOT_ACCEPTABLE, reason = "Already exists.")
@ErrorCode("SMB_CON:G409")
public class GroupAlreadyExistsException extends RuntimeException {

  /**
   * Instantiates a new group already exists exception.
   *
   * @param groupName the group name
   */
  public GroupAlreadyExistsException(String groupName) {
    super(String.format("Group [%s] already exists.", groupName));
  }

}
