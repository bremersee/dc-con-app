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

package org.bremersee.dccon.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.ldaptive.spring.boot.autoconfigure.LdaptiveProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * The kinit password file configuration.
 *
 * @author Christian Bremer
 */
@Profile("cli")
@Component
@Slf4j
public class KinitPasswordFileConfiguration {

  private final DomainControllerProperties properties;

  private final LdaptiveProperties ldaptiveProperties;

  /**
   * Instantiates a new kinit password file configuration.
   *
   * @param properties the properties
   * @param ldaptivePropertiesProvider the provider of the ldaptive properties
   */
  public KinitPasswordFileConfiguration(
      DomainControllerProperties properties,
      ObjectProvider<LdaptiveProperties> ldaptivePropertiesProvider) {
    this.properties = properties;
    this.ldaptiveProperties = ldaptivePropertiesProvider.getIfAvailable();
  }

  /**
   * Init.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void init() {
    if (ldaptiveProperties == null) {
      log.warn("Kinit password file cannot be created because ldaptive properties are not "
          + "present. You have to enable profile 'ldap'.");
    } else {
      Assert.hasText(properties.getKinitAdministratorName(),
          "Kinit administrator name must be present.");
      Assert.hasText(properties.getKinitPasswordFile(),
          "Kinit password file must be specified.");
      final File file = new File(properties.getKinitPasswordFile());
      if (!file.exists()) {
        try (final FileOutputStream out = new FileOutputStream(file)) {
          out.write(ldaptiveProperties.getBindCredentials().getBytes(StandardCharsets.UTF_8));
          out.flush();
        } catch (IOException e) {
          log.error("Creating kinit password file failed.");
        }
      }
      Assert.isTrue(file.exists(), "Kinit password file must exist.");
    }
  }

}
