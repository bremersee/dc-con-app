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

import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * The swagger configuration.
 *
 * @author Christian Bremer
 */
@Configuration
@EnableConfigurationProperties({SwaggerProperties.class})
@EnableSwagger2
public class SwaggerConfiguration {

  private SwaggerProperties swaggerProperties;

  /**
   * Instantiates a new swagger configuration.
   *
   * @param swaggerProperties the swagger properties
   */
  @Autowired
  public SwaggerConfiguration(SwaggerProperties swaggerProperties) {
    this.swaggerProperties = swaggerProperties;
  }

  /**
   * Returns the swagger docket. The swagger definition will be available under {@code
   * http://localhost:8090/v2/api-docs}*.
   *
   * @return the swagger docket
   */
  @Bean
  public Docket api() {
    return new Docket(DocumentationType.SWAGGER_2)
        .select()
        .apis(RequestHandlerSelectors.any())
        .paths(PathSelectors.ant("/api/**"))
        .build()
        .pathMapping(swaggerProperties.getPathMapping())
        .apiInfo(apiInfo());
  }

  private ApiInfo apiInfo() {
    final Contact contact;
    if (StringUtils.hasText(swaggerProperties.getContactName())
        || StringUtils.hasText(swaggerProperties.getContactUrl())
        || StringUtils.hasText(swaggerProperties.getContactEmail())) {
      contact = new Contact(
          swaggerProperties.getContactName(),
          swaggerProperties.getContactUrl(),
          swaggerProperties.getContactEmail());
    } else {
      contact = null;
    }
    return new ApiInfo(
        swaggerProperties.getTitle(),
        swaggerProperties.getDescription(),
        swaggerProperties.getVersion(),
        swaggerProperties.getTermsOfServiceUrl(),
        contact,
        swaggerProperties.getLicense(),
        swaggerProperties.getLicenseUrl(),
        Collections.emptyList());
  }

}
