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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * The message source configuration.
 *
 * @author Christian Bremer
 */
@Configuration
@EnableConfigurationProperties({
    MessageSourceProperties.class
})
@Slf4j
public class MessageSourceConfiguration {

  private static final String DEFAULT_MESSAGES_BASE_NAME = "classpath:messages";

  private final MessageSourceProperties messageSourceProperties;

  /**
   * Instantiates a new message source configuration.
   *
   * @param messageSourceProperties the message source properties
   */
  public MessageSourceConfiguration(
      final MessageSourceProperties messageSourceProperties) {
    this.messageSourceProperties = messageSourceProperties;
  }

  /**
   * Creates message source bean.
   *
   * @return the message source bean
   */
  @Bean
  public MessageSource messageSource() {
    final ReloadableResourceBundleMessageSource messageSource
        = new ReloadableResourceBundleMessageSource();
    //messageSource.setBasenames("file:/Users/cbr/messages", "classpath:messages");
    final List<String> baseNames = new ArrayList<>(messageSourceProperties.getBaseNames());
    if (!baseNames.contains(DEFAULT_MESSAGES_BASE_NAME)) {
      baseNames.add(DEFAULT_MESSAGES_BASE_NAME);
    }
    messageSource.setBasenames(baseNames.toArray(new String[0]));
    if (baseNames.size() > 1 && messageSourceProperties.getCacheSeconds() > 0) {
      messageSource.setCacheSeconds(messageSourceProperties.getCacheSeconds());
    }
    messageSource.setDefaultEncoding(messageSourceProperties.getDefaultEncoding());
    if (!messageSourceProperties.getFileEncodings().isEmpty()) {
      final Properties fileEncodings = new Properties();
      fileEncodings.putAll(messageSourceProperties.getFileEncodings());
      messageSource.setFileEncodings(fileEncodings);
    }
    messageSource.setConcurrentRefresh(messageSourceProperties.isConcurrentRefresh());
    messageSource.setFallbackToSystemLocale(messageSourceProperties.isFallbackToSystemLocale());
    messageSource.setUseCodeAsDefaultMessage(messageSourceProperties.isUseCodeAsDefaultMessage());
    return messageSource;
  }
}
