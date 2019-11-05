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
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.repository.DomainUserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Component;

/**
 * The email service implementation.
 *
 * @author Christian Bremer
 */
@Component("emailService")
@ConditionalOnProperty(name = "spring.mail.host")
@Slf4j
public class EmailServiceImpl extends AbstractEmailService {

  private JavaMailSender javaMailSender;

  /**
   * Instantiates a new email service.
   *
   * @param domainControllerProperties the domain controller properties
   * @param messageSource              the message source
   * @param userRepository             the user repository
   * @param javaMailSender             the java mail sender
   */
  public EmailServiceImpl(
      DomainControllerProperties domainControllerProperties,
      MessageSource messageSource,
      DomainUserRepository userRepository,
      JavaMailSender javaMailSender) {
    super(domainControllerProperties, messageSource, userRepository);
    this.javaMailSender = javaMailSender;
  }

  @Override
  void doSendEmailWithCredentials(
      final DomainUser domainUser,
      final Locale locale) {

    final MimeMessagePreparator preparator = mimeMessage -> {
      MimeMessageHelper helper = new MimeMessageHelper(
          mimeMessage, true, StandardCharsets.UTF_8.name());
      helper.setFrom(getDomainControllerProperties().getMailWithCredentials().getSender());
      helper.setTo(domainUser.getEmail());
      helper.setSubject(Objects.requireNonNull(getMessageSource().getMessage(
          "mail.subject.with-credentials",
          new Object[0],
          "Welcome",
          locale)));
      helper.setText(parseMailTemplate(
          loadMailTemplate(
              getDomainControllerProperties().getMailWithCredentials().getTemplateBasename(),
              locale),
          domainUser), true);
    };
    javaMailSender.send(preparator);
  }

}
