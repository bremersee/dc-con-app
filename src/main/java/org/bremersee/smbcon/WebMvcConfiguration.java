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

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.exception.RestApiExceptionMapperImpl;
import org.bremersee.exception.RestApiExceptionMapperProperties;
import org.bremersee.web.servlet.ApiExceptionResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The web mvc configuration.
 *
 * @author Christian Bremer
 */
@Configuration
@EnableConfigurationProperties({RestApiExceptionMapperProperties.class})
@Slf4j
public class WebMvcConfiguration implements WebMvcConfigurer {

  private final ApiExceptionResolver apiExceptionResolver;

  /**
   * Instantiates a new web mvc configuration.
   *
   * @param env                            the env
   * @param apiExceptionResolverProperties the api exception resolver properties
   * @param objectMapperBuilder            the object mapper builder
   */
  @Autowired
  public WebMvcConfiguration(
      final Environment env,
      final RestApiExceptionMapperProperties apiExceptionResolverProperties,
      final Jackson2ObjectMapperBuilder objectMapperBuilder) {

    final RestApiExceptionMapperImpl apiExceptionMapper = new RestApiExceptionMapperImpl(
        apiExceptionResolverProperties,
        env.getProperty("spring.application.name"));

    this.apiExceptionResolver = new ApiExceptionResolver(apiExceptionMapper);
    this.apiExceptionResolver.setObjectMapperBuilder(objectMapperBuilder);
  }

  @Override
  public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
    exceptionResolvers.add(0, apiExceptionResolver);
  }

}
