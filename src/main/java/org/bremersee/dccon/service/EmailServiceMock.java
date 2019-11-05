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
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.repository.DomainUserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * The email service mock.
 *
 * @author Christian Bremer
 */
@Component("emailServiceMock")
@ConditionalOnProperty(name = "spring.mail.host", matchIfMissing = true, havingValue = "false")
@Slf4j
public class EmailServiceMock extends AbstractEmailService {

  /**
   * Instantiates a new email service mock.
   *
   * @param domainControllerProperties the domain controller properties
   * @param messageSource              the message source
   * @param userRepository             the user repository
   */
  public EmailServiceMock(
      final DomainControllerProperties domainControllerProperties,
      final MessageSource messageSource,
      final DomainUserRepository userRepository) {
    super(domainControllerProperties, messageSource, userRepository);
  }

  /**
   * Init.
   */
  @PostConstruct
  public void init() {
    log.warn("\n"
        + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n"
        + "!! MOCK is running:  EmailServiceMock                                               !!\n"
        + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
    );
  }

  @Override
  void doSendEmailWithCredentials(
      final DomainUser domainUser,
      final Locale locale) {

    final String mailText = parseMailTemplate(loadMailTemplate(
        getDomainControllerProperties().getMailWithCredentials().getTemplateBasename(),
        locale),
        domainUser);
    log.info("Email recipient=[{}], email text=\n{}", domainUser.getEmail(), mailText);
  }

}
