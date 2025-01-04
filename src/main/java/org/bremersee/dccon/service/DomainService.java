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

import org.bremersee.dccon.model.PasswordInformation;

/**
 * The domain service interface.
 *
 * @author Christian Bremer
 */
public interface DomainService {

  /**
   * Specifies whether NIS extensions (rfc2307) are installed on the AD Domain Controller. See <a
   * href="https://wiki.samba.org/index.php/Setting_up_RFC2307_in_AD">Setting up RFC2307 in AD</a>
   */
  boolean isRfc2307Enabled();

  /**
   * Gets password information.
   *
   * @return the password information
   */
  PasswordInformation getPasswordInformation();

  /**
   * Create random password.
   *
   * @return the random password
   */
  String createRandomPassword();

}
