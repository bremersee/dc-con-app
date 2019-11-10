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
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.MailTemplateProperties.MailResolverProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ITemplateResolver;

/**
 * @author Christian Bremer
 */
@Configuration
@EnableConfigurationProperties({
    MailTemplateProperties.class
})
@Slf4j
public class MailConfiguration {

  private MailTemplateProperties properties;

  private MessageSource messageSource;

  public MailConfiguration(MailTemplateProperties properties, MessageSource messageSource) {
    this.properties = properties;
    this.messageSource = messageSource;
  }

  @PostConstruct
  public void init() {
    log.info("properties = {}", properties);
  }

  @Bean
  public TemplateEngine mailTemplateEngine() {
    final SpringTemplateEngine templateEngine = new SpringTemplateEngine();
    templateEngine.setTemplateEngineMessageSource(mailMessageSource());
    for (ITemplateResolver templateResolver : buildMailTemplateResolvers()) {
      templateEngine.addTemplateResolver(templateResolver);
    }
    return templateEngine;
  }

  protected MessageSource mailMessageSource() {
    return messageSource;
  }

  protected ResourceLoader mailResourceLoader() {
    return new DefaultResourceLoader();
  }

  protected List<ITemplateResolver> buildMailTemplateResolvers() {
    List<ITemplateResolver> resolvers = new ArrayList<>();
    int index = 1;
    for (MailResolverProperties resolverProperties : properties.getResolvers()) {
      resolvers.add(buildMailTemplateResolver(resolverProperties, index));
      index++;
    }
    return resolvers;
  }

  protected ITemplateResolver buildMailTemplateResolver(
      MailResolverProperties resolverProperties,
      int index) {

    MailTemplateResolver templateResolver = new MailTemplateResolver(mailResourceLoader());
    templateResolver.setCacheable(resolverProperties.isCacheable());
    if (!resolverProperties.getCacheablePatterns().isEmpty()) {
      templateResolver.setCacheablePatterns(resolverProperties.getCacheablePatterns());
    }
    if (resolverProperties.getCacheTtlms() != null) {
      templateResolver.setCacheTTLMs(resolverProperties.getCacheTtlms());
    }
    templateResolver.setCharacterEncoding(resolverProperties.getCharacterEncoding());
    templateResolver.setCheckExistence(resolverProperties.isCheckExistence());
    if (!resolverProperties.getCssTemplateModePatterns().isEmpty()) {
      templateResolver.setCSSTemplateModePatterns(resolverProperties.getCssTemplateModePatterns());
    }
    templateResolver.setForceSuffix(resolverProperties.isForceSuffix());
    templateResolver.setForceTemplateMode(resolverProperties.isForceTemplateMode());
    if (!resolverProperties.getHtmlTemplateModePatterns().isEmpty()) {
      templateResolver.setHtmlTemplateModePatterns(
          resolverProperties.getHtmlTemplateModePatterns());
    }
    if (!resolverProperties.getJavaScriptTemplateModePatterns().isEmpty()) {
      templateResolver.setJavaScriptTemplateModePatterns(
          resolverProperties.getJavaScriptTemplateModePatterns());
    }
    if (StringUtils.hasText(resolverProperties.getName())) {
      templateResolver.setName(resolverProperties.getName());
    }
    if (!resolverProperties.getNonCacheablePatterns().isEmpty()) {
      templateResolver.setNonCacheablePatterns(resolverProperties.getNonCacheablePatterns());
    }
    if (resolverProperties.getOrder() != null) {
      templateResolver.setOrder(resolverProperties.getOrder());
    } else {
      templateResolver.setOrder(index);
    }
    if (StringUtils.hasText(resolverProperties.getPrefix())) {
      templateResolver.setPrefix(resolverProperties.getPrefix());
    }
    if (!resolverProperties.getRawTemplateModePatterns().isEmpty()) {
      templateResolver.setRawTemplateModePatterns(resolverProperties.getRawTemplateModePatterns());
    }
    templateResolver.setResolvablePatterns(resolverProperties.resolvablePatternsOrDefault());
    if (StringUtils.hasText(resolverProperties.getSuffix())) {
      templateResolver.setSuffix(resolverProperties.getSuffix());
    }
    if (!resolverProperties.getTemplateAliases().isEmpty()) {
      templateResolver.setTemplateAliases(resolverProperties.getTemplateAliases());
    }
    if (resolverProperties.getTemplateMode() != null) {
      templateResolver.setTemplateMode(resolverProperties.getTemplateMode());
    }
    if (!resolverProperties.getTextTemplateModePatterns().isEmpty()) {
      templateResolver.setTextTemplateModePatterns(
          resolverProperties.getTextTemplateModePatterns());
    }
    templateResolver.setUseDecoupledLogic(resolverProperties.isUseDecoupledLogic());
    if (!resolverProperties.getXmlTemplateModePatterns().isEmpty()) {
      templateResolver.setXmlTemplateModePatterns(resolverProperties.getXmlTemplateModePatterns());
    }
    return templateResolver;
  }

}
