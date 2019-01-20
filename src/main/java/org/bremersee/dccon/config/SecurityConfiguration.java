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

package org.bremersee.dccon.config;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.security.authentication.KeycloakJwtConverter;
import org.bremersee.security.authentication.PasswordFlowAuthenticationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * The security configuration.
 *
 * @author Christian Bremer
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {

  /**
   * The resource server security configuration.
   */
  @Profile("!basic-auth")
  @Order(51)
  @Configuration
  @Slf4j
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

    /**
     * Init.
     */
    @PostConstruct
    public void init() {
      log.info("msg=[Using keycloak authentication.]");
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
   * The type resource server with basic auth.
   */
  @Profile("basic-auth")
  @Order(51)
  @Configuration
  @EnableConfigurationProperties(SecurityProperties.class)
  static class ResourceServerBasicAuth extends WebSecurityConfigurerAdapter {

    private final SecurityProperties securityProperties;

    /**
     * Instantiates a new resource server for basic auth.
     *
     * @param securityProperties the security properties
     */
    public ResourceServerBasicAuth(SecurityProperties securityProperties) {
      this.securityProperties = securityProperties;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http
          .antMatcher("/api/**")
          .authorizeRequests()
          .antMatchers(HttpMethod.OPTIONS).permitAll()
          .anyRequest().authenticated()
          .and()
          .cors().disable()
          .csrf().disable()
          .httpBasic()
          .realmName("smb-con")
          .and()
          .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

    @Bean
    @Override
    public UserDetailsService userDetailsService() {
      return new InMemoryUserDetailsManager(securityProperties.buildBasicAuthUserDetails());
    }
  }

  /**
   * The actuator security configuration.
   */
  @Profile("!basic-auth")
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
          .access(properties.buildAccess())
          .and()
          .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }
  }

  /**
   * The actuator security configuration for basic auth.
   */
  @Profile("basic-auth")
  @Order(52)
  @Configuration
  @EnableConfigurationProperties(SecurityProperties.class)
  static class ActuatorBasicAuth extends WebSecurityConfigurerAdapter {

    private final SecurityProperties properties;

    /**
     * Instantiates a new actuator security configuration for basic auth.
     *
     * @param properties the properties
     */
    @Autowired
    public ActuatorBasicAuth(final SecurityProperties properties) {
      this.properties = properties;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http
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
          .access(properties.buildAccess())
          .and()
          .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
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
