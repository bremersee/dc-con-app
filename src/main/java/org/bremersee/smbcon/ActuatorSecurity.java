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

package org.bremersee.smbcon;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * @author Christian Bremer
 */
@Order(101)
@Configuration
@EnableConfigurationProperties(ActuatorProperties.class)
@Slf4j
public class ActuatorSecurity extends WebSecurityConfigurerAdapter {

  private final ActuatorProperties properties;

  @Autowired
  public ActuatorSecurity(ActuatorProperties properties) {
    this.properties = properties;
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http
        .requestMatcher(EndpointRequest.toAnyEndpoint())
        .userDetailsService(userDetailsService())
        .csrf().disable()
        .httpBasic().realmName("actuator")
        .and()
        .requestMatcher(EndpointRequest.toAnyEndpoint())
        .authorizeRequests()
        .antMatchers(HttpMethod.GET, "/actuator/health").permitAll()
        .antMatchers(HttpMethod.GET, "/actuator/info").permitAll()
        .anyRequest()
        .hasAuthority("ROLE_ACTUATOR");
  }

  @Bean
  @Override
  public UserDetailsService userDetailsService() {

    log.info("Building user details service with {}", properties);
    final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    final UserDetails[] userDetails = properties.getUsers().stream().map(
        simpleUser -> User.builder()
            .username(simpleUser.getName())
            .password(simpleUser.getPassword())
            .authorities(
                simpleUser.getAuthorities().toArray(new String[simpleUser.getAuthorities().size()]))
            .passwordEncoder(encoder::encode)
            .build())
        .toArray(UserDetails[]::new);
    return new InMemoryUserDetailsManager(userDetails);
  }

}
