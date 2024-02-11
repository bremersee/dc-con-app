/*
 * Copyright 2019-2020 the original author or authors.
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

import jakarta.validation.constraints.NotNull;
import java.util.Locale;
import org.springframework.lang.Nullable;

/**
 * The email service.
 *
 * @author Christian Bremer
 */
public interface EmailService {

  /**
   * Send email with credentials.
   *
   * @param userName the username
   * @param clearPassword the clear password
   * @param language the language
   */
  void sendEmailWithCredentials(
      @NotNull String userName,
      @Nullable String clearPassword,
      @Nullable Locale language);

}
