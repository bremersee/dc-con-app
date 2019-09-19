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

import javax.validation.constraints.NotNull;
import org.bremersee.dccon.model.DomainUser;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;

/**
 * The avatar service interface.
 *
 * @author Christian Bremer
 */
@Validated
public interface AvatarService {

  /**
   * Find avatar.
   *
   * @param domainUser    the domain user
   * @param returnDefault the return default flag
   * @return the avatar bytes or {@code null} if there is no avatar
   */
  @Nullable
  byte[] findAvatar(@NotNull DomainUser domainUser, Boolean returnDefault);

}
