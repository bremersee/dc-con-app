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

import java.util.function.Supplier;
import org.bremersee.exception.annotation.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The group not found exception.
 *
 * @author Christian Bremer
 */
@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Group not found.")
@ErrorCode("SMB_CON:G404")
public class GroupNotFoundException extends NotFoundException {

  /**
   * Instantiates a new group not found exception.
   */
  private GroupNotFoundException() {
    super("Group not found.");
  }

  /**
   * Instantiates a new group not found exception.
   *
   * @param groupName the group name
   */
  private GroupNotFoundException(String groupName) {
    super(String.format("Group [%s] not found.", groupName));
  }

  /**
   * Supplier of a group not found exception.
   *
   * @param groupName the group name (can be {@code null})
   * @return the supplier
   */
  public static Supplier<GroupNotFoundException> supplier(final String groupName) {
    return () -> StringUtils.hasText(groupName)
        ? new GroupNotFoundException(groupName)
        : new GroupNotFoundException();
  }

}
