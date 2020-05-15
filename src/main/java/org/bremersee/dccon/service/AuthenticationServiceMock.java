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

import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.repository.DomainUserRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * The authentication service mock.
 *
 * @author Christian Bremer
 */
@Profile("!ldap")
@Component
@Slf4j
public class AuthenticationServiceMock implements AuthenticationService {

  private final DomainUserRepository userRepository;

  /**
   * Instantiates a new Authentication service mock.
   *
   * @param userRepository the user repository
   */
  public AuthenticationServiceMock(DomainUserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Init.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void init() {
    log.warn("\n"
        + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n"
        + "!! MOCK is running:  AuthenticationService                                          !!\n"
        + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
    );
  }

  @Override
  public boolean passwordMatches(String userName, String clearPassword) {
    return userRepository.findOne(userName)
        .map(domainUser -> clearPassword != null && clearPassword.equals(domainUser.getPassword()))
        .orElse(false);
  }

}
