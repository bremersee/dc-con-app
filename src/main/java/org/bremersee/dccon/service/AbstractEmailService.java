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

import java.util.Locale;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.common.model.TwoLetterLanguageCode;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.repository.DomainUserRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * The abstract email service.
 *
 * @author Christian Bremer
 */
@Slf4j
public abstract class AbstractEmailService implements EmailService {

  @Getter(AccessLevel.PACKAGE)
  private DomainControllerProperties properties;

  @Getter(AccessLevel.PACKAGE)
  private DomainUserRepository userRepository;

  @Getter(AccessLevel.PACKAGE)
  private TemplateEngine templateEngine;

  public AbstractEmailService(
      DomainControllerProperties properties,
      DomainUserRepository userRepository,
      TemplateEngine templateEngine) {
    this.properties = properties;
    this.userRepository = userRepository;
    this.templateEngine = templateEngine;
  }

  @Async
  @Override
  public void sendEmailWithCredentials(
      final String userName,
      final String clearPassword,
      final TwoLetterLanguageCode language) {

    if (!StringUtils.hasText(clearPassword)) {
      log.debug("No clear password is present; sending no email with credentials.");
      return;
    }
    final Locale locale = language != null ? language.toLocale() : Locale.ENGLISH;
    userRepository.findOne(userName).ifPresent(domainUser -> {
      if (StringUtils.hasText(domainUser.getEmail())) {
        domainUser.setPassword(clearPassword);
        if (!StringUtils.hasText(domainUser.getDisplayName())) {
          domainUser.setDescription(domainUser.getUserName());
        }
        final Context ctx = new Context(locale);
        ctx.setLocale(locale);
        ctx.setVariable("user", domainUser);
        ctx.setVariable("props", properties);
        ctx.setVariable("lang", locale.getLanguage());
        final String mailText = templateEngine.process(
            properties.getMailWithCredentials().getTemplateBasename(),
            ctx);
        doSendEmailWithCredentials(domainUser, locale, mailText);
      }
    });
  }

  /**
   * Do send email with credentials.
   *
   * @param domainUser the domain user
   */
  abstract void doSendEmailWithCredentials(
      @NotNull DomainUser domainUser,
      @NotNull Locale locale,
      @NotNull String mailText);
}
