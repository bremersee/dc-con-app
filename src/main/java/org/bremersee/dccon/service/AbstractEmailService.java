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

package org.bremersee.dccon.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.bremersee.common.model.TwoLetterLanguageCode;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.repository.DomainUserRepository;
import org.bremersee.exception.ServiceException;
import org.springframework.context.MessageSource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.StringUtils;

/**
 * The abstract email service.
 *
 * @author Christian Bremer
 */
@Slf4j
public abstract class AbstractEmailService implements EmailService {

  @Setter
  private ResourceLoader resourceLoader = new DefaultResourceLoader();

  @Setter
  private TemplateParserContext templateParserContext = new TemplateParserContext("${", "}");

  @Setter
  private ExpressionParser parser = new SpelExpressionParser();

  @Getter(AccessLevel.PACKAGE)
  private final DomainControllerProperties domainControllerProperties;

  @Getter(AccessLevel.PACKAGE)
  private final MessageSource messageSource;

  @Getter(AccessLevel.PACKAGE)
  private final DomainUserRepository userRepository;

  /**
   * Instantiates a new Abstract email service.
   *
   * @param domainControllerProperties the domain controller properties
   * @param messageSource              the message source
   * @param userRepository             the user repository
   */
  AbstractEmailService(
      final DomainControllerProperties domainControllerProperties,
      final MessageSource messageSource,
      final DomainUserRepository userRepository) {
    this.domainControllerProperties = domainControllerProperties;
    this.messageSource = messageSource;
    this.userRepository = userRepository;
  }

  /**
   * Load mail template string.
   *
   * @param mailBaseName the mail base name
   * @param language     the language
   * @return the string
   */
  String loadMailTemplate(
      @SuppressWarnings("SameParameterValue") final String mailBaseName,
      final Locale language) {

    final String languageSuffix = language != null && language.getLanguage() != null
        ? "_" + language.getLanguage()
        : "_" + Locale.ENGLISH.getLanguage();
    final String templateSuffix = domainControllerProperties.getMailWithCredentials()
        .getTemplateSuffix();
    final String location = mailBaseName + languageSuffix + templateSuffix;
    final Resource resource;
    if (resourceLoader.getResource(location).exists()) {
      resource = resourceLoader.getResource(location);
    } else {
      resource = resourceLoader.getResource(mailBaseName + templateSuffix);
    }
    try {
      return IOUtils.toString(resource.getInputStream(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      final ServiceException se = ServiceException.internalServerError(
          resource.getDescription() + " can not be read.", e);
      log.error("Sending email failed.", se);
      throw se;
    }
  }

  /**
   * Parse mail template string.
   *
   * @param mailTemplate the mail template
   * @param domainUser   the domain user
   * @return the string
   */
  String parseMailTemplate(final String mailTemplate, final DomainUser domainUser) {
    final Expression expression = parser.parseExpression(mailTemplate, templateParserContext);
    final StandardEvaluationContext context = new StandardEvaluationContext();
    context.setVariable("props", domainControllerProperties);
    context.setVariable("user", domainUser);
    return expression.getValue(context, String.class);
  }

  @Async
  @Override
  public void sendEmailWithCredentials(
      final String userName,
      final String clearPassword,
      final TwoLetterLanguageCode language) {

    if (!StringUtils.hasText(clearPassword)) {
      log.debug("No clear password is present; sending no email with credentials.");
    }
    final Locale locale = language != null ? language.toLocale() : Locale.ENGLISH;
    userRepository.findOne(userName).ifPresent(domainUser -> {
      if (StringUtils.hasText(domainUser.getEmail())) {
        domainUser.setPassword(clearPassword);
        doSendEmailWithCredentials(domainUser, locale);
      }
    });
  }

  /**
   * Do send email with credentials.
   *
   * @param domainUser the domain user
   * @param locale     the locale
   */
  abstract void doSendEmailWithCredentials(@NotNull DomainUser domainUser, @NotNull Locale locale);
}
