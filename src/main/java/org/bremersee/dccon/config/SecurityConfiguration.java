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
import org.bremersee.security.authentication.AccessTokenRetriever;
import org.bremersee.security.authentication.AuthenticationProperties;
import org.bremersee.security.authentication.JsonPathJwtConverter;
import org.bremersee.security.authentication.PasswordFlowAuthenticationManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * The security configuration.
 *
 * @author Christian Bremer
 */
@ConditionalOnWebApplication
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(AuthenticationProperties.class)
@Slf4j
public class SecurityConfiguration {

  private AuthenticationProperties properties;

  /**
   * Instantiates a new security configuration.
   *
   * @param properties the properties
   */
  public SecurityConfiguration(
      AuthenticationProperties properties) {
    this.properties = properties;
  }

  /**
   * Password flow authentication manager for the actuator endpoints that uses a different jwk uri
   * as the resource server.
   *
   * @param jwkUriSet the jwk uri set
   * @param jwsAlgorithm the jws algorithm
   * @param issuerUri the issuer uri
   * @param jwtAuthenticationConverter the jwt authentication converter
   * @param accessTokenRetriever the access token retriever
   * @return the password flow authentication manager
   */
  @ConditionalOnProperty(
      prefix = "bremersee.security.authentication.actuator.jwt",
      name = "jwk-set-uri")
  @Bean
  public PasswordFlowAuthenticationManager passwordFlowAuthenticationManager(
      @Value("${bremersee.security.authentication.actuator.jwt.jwk-set-uri}")
          String jwkUriSet,
      @Value("${bremersee.security.authentication.actuator.jwt.jws-algorithm:RS256}")
          String jwsAlgorithm,
      @Value("${bremersee.security.authentication.actuator.jwt.issuer-uri:}")
          String issuerUri,
      ObjectProvider<Converter<Jwt, ? extends AbstractAuthenticationToken>> jwtAuthenticationConverter,
      ObjectProvider<AccessTokenRetriever<String>> accessTokenRetriever) {

    log.info("Creating password flow authentication manager with jwk uri {}", jwkUriSet);
    NimbusJwtDecoder nimbusJwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkUriSet)
        .jwsAlgorithm(SignatureAlgorithm.from(jwsAlgorithm)).build();
    if (StringUtils.hasText(issuerUri)) {
      nimbusJwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
    }
    return new PasswordFlowAuthenticationManager(
        properties,
        nimbusJwtDecoder,
        jwtAuthenticationConverter.getIfAvailable(),
        accessTokenRetriever.getIfAvailable());
  }

  /**
   * The swagger security configuration.
   */
  @ConditionalOnWebApplication
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


  /**
   * The resource server security configuration.
   */
  @ConditionalOnWebApplication
  @ConditionalOnProperty(
      prefix = "bremersee.security.authentication",
      name = "enable-jwt-support",
      havingValue = "true")
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
          .authorizeRequests()
          .antMatchers(HttpMethod.OPTIONS).permitAll()
          .anyRequest().authenticated()
          .and()
          .oauth2ResourceServer((rs) -> rs
              .jwt()
              .jwtAuthenticationConverter(jwtConverter).and())
          .csrf().disable();
    }
  }

  /**
   * The actuator security configuration.
   */
  @ConditionalOnWebApplication
  @ConditionalOnProperty(
      prefix = "bremersee.security.authentication",
      name = "enable-jwt-support",
      havingValue = "true")
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
          .authorizeRequests()
          .antMatchers(HttpMethod.OPTIONS).permitAll()
          .requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
          .requestMatchers(EndpointRequest.to(InfoEndpoint.class)).permitAll()
          .requestMatchers(new AndRequestMatcher(
              EndpointRequest.toAnyEndpoint(),
              new AntPathRequestMatcher("/**", "GET")))
          .access(properties.getActuator().buildAccessExpression(properties::ensureRolePrefix))
          .anyRequest()
          .access(properties.getActuator().buildAdminAccessExpression(properties::ensureRolePrefix))
          .and()
          .csrf().disable()
          .authenticationProvider(passwordFlowAuthenticationManager)
          .httpBasic()
          .realmName("actuator")
          .and()
          .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }
  }

  /**
   * The in-memory security configuration with basic auth.
   */
  @ConditionalOnWebApplication
  @ConditionalOnProperty(
      prefix = "bremersee.security.authentication",
      name = "enable-jwt-support",
      havingValue = "false", matchIfMissing = true)
  @Order(51)
  @Configuration
  @Slf4j
  @EnableConfigurationProperties(AuthenticationProperties.class)
  static class InMemorySecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final AuthenticationProperties properties;

    /**
     * Instantiates a new in-memory security configuration with basic auth.
     *
     * @param properties the authentication properties
     */
    public InMemorySecurityConfiguration(AuthenticationProperties properties) {
      this.properties = properties;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      log.info("Authorizing requests to /api/** with basic auth.");
      http
          .authorizeRequests()
          .antMatchers(HttpMethod.OPTIONS).permitAll()
          .requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
          .requestMatchers(EndpointRequest.to(InfoEndpoint.class)).permitAll()
          .requestMatchers(new AndRequestMatcher(
              EndpointRequest.toAnyEndpoint(),
              new AntPathRequestMatcher("/**", "GET")))
          .access(properties.getActuator().buildAccessExpression(properties::ensureRolePrefix))
          .requestMatchers(EndpointRequest.toAnyEndpoint())
          .access(properties.getActuator().buildAdminAccessExpression(properties::ensureRolePrefix))
          .anyRequest()
          .authenticated()
          .and()
          .csrf().disable()
          .userDetailsService(userDetailsService())
          .formLogin().disable()
          .httpBasic().realmName("dc-con")
          .and()
          .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

    @Override
    protected UserDetailsService userDetailsService() {
      return new InMemoryUserDetailsManager(properties.buildBasicAuthUserDetails());
    }
  }

}
