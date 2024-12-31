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

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.model.AvatarDefault;
import org.bremersee.dccon.repository.img.ImageUtils;
import org.bremersee.exception.ServiceException;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * The fallback avatar repository.
 *
 * @author Christian Bremer
 */
@Component
@Order(AvatarProvider.FALLBACK_AVATAR_PROVIDER_ORDER)
@Slf4j
public class FallbackAvatarProvider implements AvatarProvider {

  private static final ResourceLoader RESOURCE_LOADER = new DefaultResourceLoader();

  private static final String AVATAR_LOCATION = "classpath:static/mp.jpg";

  /**
   * Get fallback avatar.
   *
   * @param size the size
   * @return the fallback avatar
   */
  public static byte[] getFallbackAvatar(Integer size) {
    int s = size == null || size < 1 || size > MAX_AVATAR_SIZE ? DEFAULT_AVATAR_SIZE : size;
    try {
      BufferedImage img = ImageIO
          .read(RESOURCE_LOADER.getResource(AVATAR_LOCATION).getInputStream());
      BufferedImage scaledImg = ImageUtils
          .scaleImage(img, new Dimension(s, s));
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ImageIO.write(scaledImg, "JPG", out);
      return out.toByteArray();

    } catch (IOException e) {
      throw ServiceException.internalServerError(
          "Getting default avatar failed.",
          "org.bremersee:dc-con-app:1ec0dda8-7358-4e1c-a8f2-f4bd64e439f0",
          e);
    }
  }

  @Override
  public Optional<byte[]> findAvatar(String user, AvatarDefault avatarDefault, Integer size) {
    AvatarDefault defaultAvatar = requireNonNullElse(avatarDefault, AvatarDefault.NOT_FOUND);
    if (AvatarDefault.NOT_FOUND.equals(defaultAvatar)) {
      return Optional.empty();
    }
    return Optional.of(getFallbackAvatar(getAvatarSize(size)))
        .filter(this::isNotEmpty);
  }
}
