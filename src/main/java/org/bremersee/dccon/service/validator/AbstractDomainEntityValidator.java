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

package org.bremersee.dccon.service.validator;

import static org.bremersee.dccon.ErrorCode.EC_SAM_ACCOUNT_ALREADY_EXISTS;

import lombok.AccessLevel;
import lombok.Getter;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.SamAccount;
import org.bremersee.dccon.repository.DomainRepository;
import org.bremersee.exception.ServiceException;

/**
 * The abstract domain entity validator.
 *
 * @author Christian Bremer
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractDomainEntityValidator {

  @Getter(AccessLevel.PROTECTED)
  private final DomainControllerProperties properties;

  @Getter(AccessLevel.PROTECTED)
  private final DomainRepository domainRepository;

  /**
   * Instantiates a new abstract domain entity validator.
   *
   * @param properties the properties
   */
  protected AbstractDomainEntityValidator(
      DomainControllerProperties properties,
      DomainRepository domainRepository) {
    this.properties = properties;
    this.domainRepository = domainRepository;
  }

  /**
   * Determine whether the given name is already in use.
   *
   * @param name the name
   * @return the boolean
   */
  protected boolean samAccountNameExists(String name) {
    return false;
  }

  /**
   * Throws an already exists exception if the name is already in use.
   *
   * @param name the name
   * @param domainClass the domain class
   */
  protected void validateSamAccountNameNotExists(
      String name, Class<? extends SamAccount> domainClass) {
    if (samAccountNameExists(name)) {
      throw ServiceException.alreadyExistsWithErrorCode(
          domainClass.getSimpleName(),
          name,
          EC_SAM_ACCOUNT_ALREADY_EXISTS);
    }
  }

}
