package org.bremersee.dccon.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.UUID;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainUser;
import org.junit.Test;

/**
 * The gravatar service test.
 *
 * @author Christian Bremer
 */
public class GravatarServiceTest {

  /**
   * Find avatar.
   */
  @Test
  public void findAvatar() {
    DomainControllerProperties properties = new DomainControllerProperties();
    GravatarService service = new GravatarService(properties);

    byte[] gravatar = service.findAvatar(DomainUser.builder()
            .email(UUID.randomUUID() + "@bremersee.org")
            .build(),
        false);
    assertNull(gravatar);

    service = new GravatarService(properties);

    gravatar = service.findAvatar(DomainUser.builder()
        .email(UUID.randomUUID() + "@bremersee.org")
        .build(), true);
    assertNotNull(gravatar); // default image
  }
}