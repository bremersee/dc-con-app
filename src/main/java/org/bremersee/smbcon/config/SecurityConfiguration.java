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

package org.bremersee.smbcon.config;

import org.bremersee.security.authentication.KeycloakJwtConverter;
import org.bremersee.security.authentication.PasswordFlowAuthenticationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * The security configuration.
 *
 * @author Christian Bremer
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

  /**
   * The resource server security configuration.
   */
  @Order(51)
  @Configuration
  static class ResourceServer extends WebSecurityConfigurerAdapter {

    private KeycloakJwtConverter keycloakJwtConverter;

    /**
     * Instantiates a new resource server security configuration.
     *
     * @param keycloakJwtConverter the keycloak jwt converter
     */
    @Autowired
    public ResourceServer(
        KeycloakJwtConverter keycloakJwtConverter) {
      this.keycloakJwtConverter = keycloakJwtConverter;
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
      http
          .antMatcher("/api/**")
          .authorizeRequests()
          .antMatchers(HttpMethod.OPTIONS).permitAll()
          .anyRequest().authenticated()
          .and()
          .oauth2ResourceServer()
          .jwt()
          .jwtAuthenticationConverter(keycloakJwtConverter);
    }
  }

  /**
   * The actuator security configuration.
   */
  @Order(52)
  @Configuration
  @EnableConfigurationProperties(SecurityProperties.class)
  static class Actuator extends WebSecurityConfigurerAdapter {

    private final SecurityProperties properties;

    private final PasswordFlowAuthenticationManager passwordFlowAuthenticationManager;

    /**
     * Instantiates a new actuator security configuration.
     *
     * @param properties                        the properties
     * @param passwordFlowAuthenticationManager the password flow authentication manager
     */
    @Autowired
    public Actuator(
        final SecurityProperties properties,
        final PasswordFlowAuthenticationManager passwordFlowAuthenticationManager) {
      this.properties = properties;
      this.passwordFlowAuthenticationManager = passwordFlowAuthenticationManager;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http
          .authenticationProvider(passwordFlowAuthenticationManager)
          .requestMatcher(EndpointRequest.toAnyEndpoint())
          .cors().disable()
          .csrf().disable()
          .httpBasic()
          .realmName("actuator")
          .and()
          .requestMatcher(EndpointRequest.toAnyEndpoint())
          .authorizeRequests()
          .requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
          .requestMatchers(EndpointRequest.to(InfoEndpoint.class)).permitAll()
          .anyRequest()
          .access(properties.buildAccess());
    }
  }

  /**
   * The swagger security configuration.
   */
  @Order(53)
  @Configuration
  static class Swagger extends WebSecurityConfigurerAdapter {

    @Override
    public void configure(WebSecurity web) {
      web.ignoring().antMatchers(
          "/v2/api-docs",
          "/v2/api-docs/**");
    }
  }

}
