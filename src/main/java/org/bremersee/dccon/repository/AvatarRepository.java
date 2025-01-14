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
import jakarta.validation.constraints.NotNull;
import java.io.InputStream;
import java.util.Optional;
import org.bremersee.dccon.model.AvatarDefault;
import org.bremersee.dccon.model.TreeSearchScope;
import org.ldaptive.dn.Dn;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;

/**
 * The avatar repository.
 *
 * @author Christian Bremer
 */
@Validated
public interface AvatarRepository {

  /**
   * The constant DEFAULT_AVATAR_SIZE.
   */
  int DEFAULT_AVATAR_SIZE = AvatarProvider.DEFAULT_AVATAR_SIZE;

  /**
   * The constant MAX_AVATAR_SIZE.
   */
  int MAX_AVATAR_SIZE = AvatarProvider.MAX_AVATAR_SIZE;

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
  default boolean isAvatarNotEmpty(byte[] bytes) {
    return bytes != null && bytes.length > 0;
  }

  /**
   * Exists avatar.
   *
   * @param user the user
   * @return the boolean
   */
  default boolean existsAvatar(
      @NotEmpty String user,
      @Nullable Dn ou,
      @Nullable TreeSearchScope searchScope) {

    return findAvatar(user, ou, searchScope, AvatarDefault.NOT_FOUND, DEFAULT_AVATAR_SIZE)
        .isPresent();
  }

  boolean existsAvatarInActiveDirectory(
      @NotEmpty String user,
      @Nullable Dn ou,
      @Nullable TreeSearchScope searchScope);

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
      @Nullable Dn ou,
      @Nullable TreeSearchScope searchScope,
      @Nullable AvatarDefault avatarDefault,
      @Nullable Integer size);

  /**
   * Save avatar.
   *
   * @param userName the username
   * @param avatar the avatar
   */
  void saveAvatar(@NotEmpty String userName, @NotNull InputStream avatar);

  /**
   * Remove avatar.
   *
   * @param userName the username
   */
  void removeAvatar(@NotEmpty String userName);

}
