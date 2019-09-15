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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.data.ldaptive.LdaptiveProperties;
import org.springframework.context.annotation.Profile;
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

  private final LdaptiveProperties adProperties;

  /**
   * Instantiates a new kinit password file configuration.
   *
   * @param properties   the properties
   * @param adProperties the ad properties
   */
  public KinitPasswordFileConfiguration(
      DomainControllerProperties properties,
      LdaptiveProperties adProperties) {
    this.properties = properties;
    this.adProperties = adProperties;
  }

  /**
   * Init.
   */
  @PostConstruct
  public void init() {
    Assert.hasText(properties.getKinitAdministratorName(),
        "Kinit administrator name must be present.");
    Assert.hasText(properties.getKinitPasswordFile(),
        "Kinit password file must be specified.");
    final File file = new File(properties.getKinitPasswordFile());
    if (!file.exists()) {
      try (final FileOutputStream out = new FileOutputStream(file)) {
        out.write(adProperties.getBindCredential().getBytes(StandardCharsets.UTF_8));
        out.flush();
      } catch (IOException e) {
        log.error("Creating kinit password file failed.");
      }
    }
    Assert.isTrue(file.exists(), "Kinit password file must exist.");
  }

}
