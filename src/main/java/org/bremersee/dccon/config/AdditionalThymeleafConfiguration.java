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

import org.bremersee.thymeleaf.AbstractAdditionalThymeleafConfiguration;
import org.bremersee.thymeleaf.AdditionalThymeleafProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * The additional thymeleaf configuration.
 *
 * @author Christian Bremer
 */
@Configuration
@EnableConfigurationProperties({
    AdditionalThymeleafProperties.class
})
public class AdditionalThymeleafConfiguration extends AbstractAdditionalThymeleafConfiguration {

  /**
   * Instantiates a new additional thymeleaf configuration.
   *
   * @param applicationContext the application context
   * @param properties         the properties
   */
  public AdditionalThymeleafConfiguration(
      ApplicationContext applicationContext,
      AdditionalThymeleafProperties properties) {
    super(applicationContext, properties);
  }
}
