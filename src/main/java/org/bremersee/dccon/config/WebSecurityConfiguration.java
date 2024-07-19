/*
 * Copyright 2024 the original author or authors.
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

import static org.springframework.util.ObjectUtils.isEmpty;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatchers;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * The type WebSecurityConfiguration.
 *
 * @author Christian Bremer
 */
@EnableWebSecurity
@EnableConfigurationProperties({
    OAuth2ResourceServerProperties.class
})
@Configuration
public class WebSecurityConfiguration {

  private final Environment env;

  private final OAuth2ResourceServerProperties resourceServerProperties;

  //private final CorsConfigurationSource corsConfigurationSource;

  private final Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

  public WebSecurityConfiguration(
      Environment env,
      OAuth2ResourceServerProperties resourceServerProperties,
      //CorsConfigurationSource corsConfigurationSource,
      ObjectProvider<Converter<Jwt, AbstractAuthenticationToken>> jwtConverterProvider) {
    this.env = env;
    this.resourceServerProperties = resourceServerProperties;
    //this.corsConfigurationSource = corsConfigurationSource;
    this.jwtAuthenticationConverter = jwtConverterProvider
        .getIfAvailable(JwtAuthenticationConverter::new);
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    String appName = env.getProperty("spring.application.name", "config-server");
    http
        .authorizeHttpRequests(customizer -> customizer
            .requestMatchers(HttpMethod.OPTIONS, "/**")
            .permitAll()

            .requestMatchers(
                EndpointRequest.to(InfoEndpoint.class),
                EndpointRequest.to(HealthEndpoint.class))
            .permitAll()

            .requestMatchers(EndpointRequest.toAnyEndpoint())
            .hasAuthority("ROLE_ACTUATOR_ADMIN")

            .requestMatchers("/css/**").permitAll()
            .requestMatchers("/fonts/**").permitAll()
            .requestMatchers("/lib/**").permitAll()

            .requestMatchers("/login").permitAll()
            .requestMatchers("/logged-out").permitAll()
            .requestMatchers("/index.html").permitAll()

            .requestMatchers("/v3/**").permitAll()
            .requestMatchers("/webjars/**").permitAll()
            .requestMatchers("/swagger-ui/**").permitAll()
            .requestMatchers("/swagger-ui.html").permitAll()

            .anyRequest().authenticated())

        .csrf(customizer -> customizer
            .ignoringRequestMatchers(RequestMatchers.anyOf(
                EndpointRequest.toAnyEndpoint(),
                new AntPathRequestMatcher("/api/**"))))

//        .cors(customizer -> customizer
//            .configurationSource(corsConfigurationSource))

        .headers(configurer -> configurer
            .frameOptions(FrameOptionsConfig::sameOrigin))

        .sessionManagement(customizer -> customizer
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

        .httpBasic(customizer -> customizer.realmName(appName))

        .formLogin(form -> form
            .loginPage("/login")
            .loginProcessingUrl("/login")
            .defaultSuccessUrl("/profile")
            .permitAll())
        .logout(logout -> logout
            .logoutUrl("/logged-out")
            .permitAll())

        //.formLogin(Customizer.withDefaults())
        ;

    if (!isEmpty(resourceServerProperties.getJwt().getJwkSetUri())) {
      http
          .oauth2ResourceServer(configurer -> configurer
              .jwt(jwtConfigurer -> jwtConfigurer
                  .jwtAuthenticationConverter(jwtAuthenticationConverter)
                  .jwkSetUri(resourceServerProperties.getJwt().getJwkSetUri())));
    }

    return http.build();
  }

}
