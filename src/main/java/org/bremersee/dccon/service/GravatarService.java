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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainUser;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

/**
 * The gravatar service.
 *
 * @author Christian Bremer
 */
@Component("gravatarService")
@Slf4j
public class GravatarService implements AvatarService {

  private DomainControllerProperties properties;

  /**
   * Instantiates a gravatar service.
   *
   * @param properties the properties
   */
  public GravatarService(DomainControllerProperties properties) {
    this.properties = properties;
  }

  @Override
  public byte[] findAvatar(final DomainUser domainUser, final Boolean returnDefault) {
    if (!StringUtils.hasText(domainUser.getEmail())) {
      return null;
    }
    byte[] md5 = DigestUtils.md5Digest(domainUser.getEmail().getBytes(StandardCharsets.UTF_8));
    String hex = new String(Hex.encode(md5));
    byte[] response = findAvatar(properties.getGravatarUrl().replace("{}", hex));
    if ((response == null || response.length == 0) && Boolean.TRUE.equals(returnDefault)) {
      response = findAvatar(properties.getGravatarUrlForDefault());
    }
    if (response == null || response.length == 0) {
      return null;
    }
    return response;
  }

  private byte[] findAvatar(final String url) {
    try {
      byte[] response = IOUtils.toByteArray(
          new URL(url));
      if (response == null || response.length == 0) {
        return null;
      }
      return response;

    } catch (Exception e) {
      if (e instanceof MalformedURLException) {
        log.error("msg=[Getting avatar from gravatar.com failed (Malformed URL). Returning null.]",
            e);
      } else if ((e instanceof IOException) || (e instanceof NullPointerException)) { // not found
        log.debug("msg=[Getting avatar from gravatar.com failed (404). Returning null.]");
      } else {
        log.error("msg=[Getting avatar from gravatar.com failed. Returning null.]", e);
      }
      return null;
    }
  }

}
