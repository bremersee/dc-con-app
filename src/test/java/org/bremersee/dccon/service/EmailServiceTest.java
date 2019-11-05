package org.bremersee.dccon.service;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Optional;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.repository.DomainUserRepository;
import org.junit.Test;
import org.springframework.context.MessageSource;

/**
 * The email service test.
 *
 * @author Christian Bremer
 */
public class EmailServiceTest {

  /**
   * Send email with credentials.
   */
  @Test
  public void sendEmailWithCredentials() {
    DomainUser user = DomainUser.builder()
        .userName("anna")
        .displayName("Anna Livia Plurabelle")
        .email("anna@bremersee.org")
        .password("xyZ234567")
        .build();

    DomainControllerProperties domainControllerProperties = new DomainControllerProperties();

    MessageSource messageSource = mock(MessageSource.class);
    when(messageSource.getMessage(anyString(), any(), anyString(), any(Locale.class)))
        .thenReturn("Mail with credentials subject");

    DomainUserRepository userRepository = mock(DomainUserRepository.class);
    when(userRepository.findOne(anyString()))
        .thenReturn(Optional.of(user));

    EmailServiceMock emailService = new EmailServiceMock(
        domainControllerProperties,
        messageSource,
        userRepository);

    final String template = emailService
        .loadMailTemplate(domainControllerProperties.getMailWithCredentials().getTemplateBasename(),
            Locale.ENGLISH);
    // System.out.println("Template:\n" + template + "\n");
    final String mailText = emailService.parseMailTemplate(template, user);
    System.out.println("Mail:\n" + mailText + "\n");
    assertTrue(mailText.contains(user.getDisplayName()));
    assertTrue(mailText.contains(user.getUserName()));
    assertTrue(mailText.contains(user.getPassword()));
    assertTrue(mailText.contains(domainControllerProperties.getCompanyName()));
  }
}