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

import jakarta.validation.constraints.NotEmpty;
import java.util.Optional;
import org.bremersee.dccon.model.AvatarDefault;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;

/**
 * The avatar provider.
 *
 * @author Christian Bremer
 */
@Validated
public interface AvatarProvider {

  /**
   * The constant FALLBACK_AVATAR_REPOSITORY_ORDER.
   */
  int FALLBACK_AVATAR_PROVIDER_ORDER = Integer.MAX_VALUE - 1;

  /**
   * The constant GRAVATAR_REPOSITORY_ORDER.
   */
  int GRAVATAR_PROVIDER_ORDER = FALLBACK_AVATAR_PROVIDER_ORDER - 100;

  /**
   * The constant DEFAULT_AVATAR_SIZE.
   */
  int DEFAULT_AVATAR_SIZE = 80;

  /**
   * The constant MAX_AVATAR_SIZE.
   */
  int MAX_AVATAR_SIZE = 2048;

  /**
   * Gets avatar size.
   *
   * @param size the size
   * @return the avatar size
   */
  default int getAvatarSize(Integer size) {
    return size == null || size < 1 || size > MAX_AVATAR_SIZE ? DEFAULT_AVATAR_SIZE : size;
  }

  /**
   * Is not empty.
   *
   * @param bytes the bytes
   * @return the boolean
   */
  default boolean isNotEmpty(byte[] bytes) {
    return bytes != null && bytes.length > 0;
  }

  /**
   * Exists avatar.
   *
   * @param user the user
   * @return the boolean
   */
  default boolean existsAvatar(@NotEmpty String user) {
    return findAvatar(user, AvatarDefault.NOT_FOUND, DEFAULT_AVATAR_SIZE).isPresent();
  }

  /**
   * Find avatar.
   *
   * @param user the user
   * @param avatarDefault the avatar default
   * @param size the size
   * @return the optional avatar
   */
  Optional<byte[]> findAvatar(
      @NotEmpty String user,
      @Nullable AvatarDefault avatarDefault,
      @Nullable Integer size);

}
