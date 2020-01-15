/*
 * Copyright 2019 the original author or authors.
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

import lombok.extern.slf4j.Slf4j;
import org.bremersee.security.authentication.AuthenticationProperties;
import org.bremersee.security.authentication.JsonPathJwtConverter;
import org.bremersee.security.authentication.PasswordFlowAuthenticationManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
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
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.util.Assert;

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

    private JsonPathJwtConverter jwtConverter;

    /**
     * Instantiates a new resource server security configuration.
     *
     * @param jwtConverterProvider the jwt converter provider
     */
    @Autowired
    public ResourceServer(ObjectProvider<JsonPathJwtConverter> jwtConverterProvider) {
      jwtConverter = jwtConverterProvider.getIfAvailable();
      Assert.notNull(jwtConverter, "JWT converter must be present.");
    }

    /**
     * Init.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
      log.info("msg=[Using jwt authentication.]");
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
      log.info("Authorizing requests to /api/** with OAuth2.");
      http
          .requestMatcher(new NegatedRequestMatcher(EndpointRequest.toAnyEndpoint()))
          .csrf().disable()
          .authorizeRequests()
          .antMatchers(HttpMethod.OPTIONS).permitAll()
          // .antMatchers("/public-api/**").permitAll()
          .anyRequest()
          .authenticated();
      http
          .oauth2ResourceServer()
          .jwt()
          .jwtAuthenticationConverter(jwtConverter);
    }
  }

  /**
   * The type resource server with basic auth.
   */
  @Profile("basic-auth")
  @Order(51)
  @Configuration
  @Slf4j
  @EnableConfigurationProperties(AuthenticationProperties.class)
  static class ResourceServerBasicAuth extends WebSecurityConfigurerAdapter {

    private final AuthenticationProperties properties;

    /**
     * Instantiates a new resource server for basic auth.
     *
     * @param properties the authentication properties
     */
    public ResourceServerBasicAuth(AuthenticationProperties properties) {
      this.properties = properties;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      log.info("Authorizing requests to /api/** with basic auth.");
      http
          .requestMatcher(new NegatedRequestMatcher(EndpointRequest.toAnyEndpoint()))
          .csrf().disable()
          .formLogin().disable()
          .httpBasic().realmName("dc-con")
          .and()
          .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
          .and()
          .antMatcher("/api/**")
          .authorizeRequests()
          .antMatchers(HttpMethod.OPTIONS).permitAll()
          .anyRequest()
          .authenticated();
    }

    @Bean
    @Override
    public UserDetailsService userDetailsService() {
      return new InMemoryUserDetailsManager(properties.buildBasicAuthUserDetails());
    }
  }

  /**
   * The actuator security configuration.
   */
  @Profile("!basic-auth")
  @Order(52)
  @Configuration
  @Slf4j
  @EnableConfigurationProperties(AuthenticationProperties.class)
  static class Actuator extends WebSecurityConfigurerAdapter {

    private final AuthenticationProperties properties;

    private final PasswordFlowAuthenticationManager passwordFlowAuthenticationManager;

    /**
     * Instantiates a new actuator security configuration.
     *
     * @param properties the properties
     * @param passwordFlowAuthenticationManager the password flow authentication manager
     */
    @Autowired
    public Actuator(
        AuthenticationProperties properties,
        ObjectProvider<PasswordFlowAuthenticationManager> passwordFlowAuthenticationManager) {
      this.properties = properties;
      this.passwordFlowAuthenticationManager = passwordFlowAuthenticationManager.getIfAvailable();
      Assert.notNull(
          this.passwordFlowAuthenticationManager,
          "Password flow authentication manager must be present.");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      log.info("Authorizing requests to /actuator/** with password flow auth.");
      http
          .requestMatcher(EndpointRequest.toAnyEndpoint())
          .csrf().disable()
          .authenticationProvider(passwordFlowAuthenticationManager)
          .httpBasic()
          .realmName("actuator")
          .and()
          .requestMatcher(EndpointRequest.toAnyEndpoint())
          .authorizeRequests()
          .antMatchers(HttpMethod.OPTIONS).permitAll()
          .requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
          .requestMatchers(EndpointRequest.to(InfoEndpoint.class)).permitAll()
          .anyRequest()
          .access(properties.getActuator().buildAccessExpression())
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
  @Slf4j
  @EnableConfigurationProperties(AuthenticationProperties.class)
  static class ActuatorBasicAuth extends WebSecurityConfigurerAdapter {

    private final AuthenticationProperties properties;

    /**
     * Instantiates a new actuator security configuration for basic auth.
     *
     * @param properties the properties
     */
    @Autowired
    public ActuatorBasicAuth(final AuthenticationProperties properties) {
      this.properties = properties;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      log.info("Authorizing requests to /actuator/** with basic auth.");
      http
          .requestMatcher(EndpointRequest.toAnyEndpoint())
          .csrf().disable()
          .httpBasic()
          .realmName("actuator")
          .and()
          .requestMatcher(EndpointRequest.toAnyEndpoint())
          .authorizeRequests()
          .antMatchers(HttpMethod.OPTIONS).permitAll()
          .requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
          .requestMatchers(EndpointRequest.to(InfoEndpoint.class)).permitAll()
          .anyRequest()
          .access(properties.getActuator().buildAccessExpression())
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
