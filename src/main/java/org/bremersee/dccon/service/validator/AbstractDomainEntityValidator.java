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

package org.bremersee.dccon.service.validator;

import lombok.AccessLevel;
import lombok.Getter;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.repository.DomainGroupRepository;
import org.bremersee.dccon.repository.DomainUserRepository;
import org.bremersee.exception.ServiceException;

/**
 * The abstract domain entity validator.
 *
 * @author Christian Bremer
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractDomainEntityValidator {

  @Getter(AccessLevel.PROTECTED)
  private DomainControllerProperties properties;

  @Getter(AccessLevel.PROTECTED)
  private DomainGroupRepository groupRepository;

  @Getter(AccessLevel.PROTECTED)
  private DomainUserRepository userRepository;

  /**
   * Instantiates a new abstract domain entity validator.
   *
   * @param properties      the properties
   * @param groupRepository the group repository
   * @param userRepository  the user repository
   */
  protected AbstractDomainEntityValidator(
      DomainControllerProperties properties,
      DomainGroupRepository groupRepository,
      DomainUserRepository userRepository) {
    this.properties = properties;
    this.groupRepository = groupRepository;
    this.userRepository = userRepository;
  }

  /**
   * Determine whether the given name is already in use.
   *
   * @param name the name
   * @return the boolean
   */
  protected boolean nameExists(String name) {
    return groupRepository.exists(name) || userRepository.exists(name);
  }

  /**
   * Throws an already exception if the name is already in use.
   *
   * @param name        the name
   * @param domainClass the domain class
   */
  protected void validateNameNotExists(String name, Class<?> domainClass) {
    if (nameExists(name)) {
      throw ServiceException.alreadyExistsWithErrorCode(
          domainClass.getSimpleName(),
          name,
          "org.bremersee:dc-con-app:7bca7443-19f3-4d44-9607-118b10882b92");
    }
  }

}
