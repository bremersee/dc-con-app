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

package org.bremersee.dccon.exception;

import java.util.function.Supplier;
import org.bremersee.exception.annotation.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The user not found exception.
 *
 * @author Christian Bremer
 */
@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "User not found.")
@ErrorCode("DC_CON:U404")
public class UserNotFoundException extends NotFoundException {

  /**
   * Instantiates a new user not found exception.
   */
  private UserNotFoundException() {
  }

  /**
   * Instantiates a new user not found exception.
   *
   * @param userName the user name
   */
  private UserNotFoundException(String userName) {
    super(String.format("User [%s] was not found.", userName));
  }

  /**
   * Supplier of a user not found exception.
   *
   * @param userName the user name (can be {@code null})
   * @return the supplier
   */
  public static Supplier<UserNotFoundException> supplier(final String userName) {
    return () -> StringUtils.hasText(userName)
        ? new UserNotFoundException(userName)
        : new UserNotFoundException();
  }

}
