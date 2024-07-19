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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.spring.security.authentication.ldaptive.LdaptiveAuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * The authentication service.
 *
 * @author Christian Bremer
 */
@Component("authenticationService")
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

  private final AuthenticationProvider authenticationProvider;

  public AuthenticationServiceImpl(List<AuthenticationProvider> authenticationProviders) {
    this.authenticationProvider = Stream.ofNullable(authenticationProviders)
        .flatMap(Collection::stream)
        .filter(authProvider -> nonNull(authProvider)
            && authProvider.supports(UsernamePasswordAuthenticationToken.class))
        .min(((o1, o2) -> {
          if (o1 instanceof LdaptiveAuthenticationManager) {
            return -1;
          }
          if (o2 instanceof LdaptiveAuthenticationManager) {
            return 1;
          }
          return 0;
        }))
        .orElse(null);
    if (isNull(authenticationProvider)) {
      log.warn("No authentication provider found.");
    } else {
      log.info("Found authentication provider [{}].", authenticationProvider.getClass().getName());
    }
  }

  @Override
  public boolean passwordMatches(final String userName, final String clearPassword) {
    if (isNull(authenticationProvider)) {
      throw new IllegalStateException("Authentication provider not set.");
    }
    try {
      Authentication authentication = authenticationProvider
          .authenticate(new UsernamePasswordAuthenticationToken(userName, clearPassword));
      return nonNull(authentication) && authentication.isAuthenticated();
    } catch (AuthenticationException e) {
      return false;
    }
  }

}
