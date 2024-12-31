/*
 * Copyright 2024 the original author or authors.
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

package org.bremersee.dccon.repository;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.AvatarDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The gravatar repository test.
 */
@ExtendWith(SoftAssertionsExtension.class)
class GravatarProviderTest {

  /**
   * The Target.
   */
  static final GravatarProvider target = new GravatarProvider(new DomainControllerProperties());

  /**
   * Find avatar.
   */
  @Test
  void findAvatar() {
    String user = "bremersee@googlemail.com";
    Optional<byte[]> actual = target.findAvatar(user, AvatarDefault.NOT_FOUND, 140);
    assertTrue(actual.isPresent());
  }

  /**
   * Find avatar of unknown user.
   *
   * @param softly the softly
   */
  @Test
  void findAvatarOfUnknownUser(SoftAssertions softly) {
    for (AvatarDefault avatarDefault : AvatarDefault.values()) {
      Optional<byte[]> actual = target.findAvatar(null, avatarDefault, 80);
      if (!AvatarDefault.NOT_FOUND.equals(avatarDefault)) {
        softly
            .assertThat(actual)
            .isPresent();
      } else {
        softly
            .assertThat(actual)
            .isEmpty();
      }
    }
  }

}