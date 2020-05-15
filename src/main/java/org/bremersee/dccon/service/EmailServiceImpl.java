/*
 * Copyright 2019-2020 the original author or authors.
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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.config.DomainControllerProperties.MailInlineAttachment;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.repository.DomainUserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.thymeleaf.TemplateEngine;

/**
 * The email service implementation.
 *
 * @author Christian Bremer
 */
@Component("emailService")
@ConditionalOnProperty(name = "spring.mail.host")
@Slf4j
public class EmailServiceImpl extends AbstractEmailService {

  private final ResourceLoader resourceLoader;

  private final MessageSource messageSource;

  private final JavaMailSender javaMailSender;

  /**
   * Instantiates a new email service.
   *
   * @param properties the properties
   * @param userRepository the user repository
   * @param templateEngine the template engine
   * @param resourceLoader the resource loader
   * @param messageSource the message source
   * @param javaMailSenderProvider the java mail sender provider
   */
  public EmailServiceImpl(
      DomainControllerProperties properties,
      DomainUserRepository userRepository,
      TemplateEngine templateEngine,
      ResourceLoader resourceLoader,
      MessageSource messageSource,
      ObjectProvider<JavaMailSender> javaMailSenderProvider) {
    super(properties, userRepository, templateEngine);
    this.resourceLoader = resourceLoader;
    this.messageSource = messageSource;
    this.javaMailSender = javaMailSenderProvider.getIfAvailable();
    Assert.notNull(this.javaMailSender, "Java mail sender must be present.");
  }

  @Override
  void doSendEmailWithCredentials(
      final DomainUser domainUser,
      final Locale locale,
      final String mailText) {

    final List<MailInlineAttachment> inlineAttachments = getProperties()
        .getMailWithCredentials()
        .getInlineAttachments();

    final MimeMessagePreparator preparator = mimeMessage -> {
      MimeMessageHelper helper = new MimeMessageHelper(
          mimeMessage, true, StandardCharsets.UTF_8.name());
      helper.setFrom(getProperties().getMailWithCredentials().getSender());
      helper.setTo(domainUser.getEmail());
      helper.setSubject(Objects.requireNonNull(messageSource.getMessage(
          "mail-with-credentials.subject",
          new Object[]{domainUser.getDisplayName()},
          "Welcome",
          locale)));
      helper.setText(mailText, true);

      for (MailInlineAttachment attachment : inlineAttachments) {
        helper.addInline(attachment.getContentId(),
            resourceLoader.getResource(attachment.getLocation()),
            attachment.getMimeType());
      }
    };
    javaMailSender.send(preparator);
  }

}
