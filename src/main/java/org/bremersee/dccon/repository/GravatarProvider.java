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

import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.AvatarDefault;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

/**
 * The gravatar repository.
 *
 * @author Christian Bremer
 */
@Component
@Order(AvatarProvider.GRAVATAR_PROVIDER_ORDER)
@Slf4j
public class GravatarProvider implements AvatarProvider {

  private final DomainControllerProperties properties;

  public GravatarProvider(DomainControllerProperties properties) {
    this.properties = properties;
  }

  @Override
  public Optional<byte[]> findAvatar(String user, AvatarDefault avatarDefault, Integer size) {
    String source = requireNonNullElseGet(user, () -> String.valueOf(UUID.randomUUID()));
    byte[] md5 = DigestUtils.md5Digest(source.getBytes(StandardCharsets.UTF_8));
    String hex = new String(Hex.encode(md5));
    AvatarDefault defaultAvatar = requireNonNullElse(avatarDefault, AvatarDefault.NOT_FOUND);
    int avatarSize = getAvatarSize(size);
    String url = properties.getUser().getGravatarUrl()
        .replace("{hash}", hex)
        .replace("{default}", String.valueOf(defaultAvatar))
        .replace("{size}", String.valueOf(avatarSize));
    try {
      return Optional.ofNullable(IOUtils.toByteArray(new URL(url)))
          .filter(this::isNotEmpty);

    } catch (Exception e) {
      if (!AvatarDefault.NOT_FOUND.equals(defaultAvatar)) {
        log.error("Getting avatar from gravatar (url = '{}') failed. This should not happen.",
            url, e);
      }
    }
    return Optional.empty();
  }

}
