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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * The email template engine test.
 *
 * @author Christian Bremer
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles({"basic-auth"})
public class EmailTemplateEngineTest {

  @Autowired
  private DomainControllerProperties domainControllerProperties;

  /**
   * The template engine.
   */
  @Autowired
  TemplateEngine templateEngine;

  /**
   * Mail with credentials test.
   */
  @Test
  public void mailWithCredentialsTest() {
    final DomainUser domainUser = DomainUser.builder()
        .displayName("Anna Livia Plurabelle")
        .firstName("Anna")
        .email("anna@example.org")
        .userName("anna")
        .password("changeit")
        .build();
    final String expected = "<span>Hello, " + domainUser.getFirstName() + "!</span>";
    final Locale locale = Locale.ENGLISH;
    final Context ctx = new Context(locale);
    ctx.setVariable("user", domainUser);
    ctx.setVariable("props", domainControllerProperties);
    ctx.setVariable("lang", locale.getLanguage());
    final String mailText = templateEngine.process(
        domainControllerProperties.getMailWithCredentials().getTemplateBasename(),
        ctx);
    System.out.println("Mail text:\n" + mailText);
    assertTrue(mailText.contains(expected));
  }

}
