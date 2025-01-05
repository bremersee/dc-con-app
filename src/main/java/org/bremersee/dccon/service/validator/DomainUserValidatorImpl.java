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

package org.bremersee.dccon.service.validator;

import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.repository.DomainRepository;
import org.springframework.stereotype.Component;

/**
 * The default domain user validator.
 */
// TODO delete
@Component("domainUserValidator")
public class DomainUserValidatorImpl extends AbstractDomainEntityValidator
    implements DomainUserValidator {

  /**
   * Instantiates a new default domain user validator.
   *
   * @param properties the properties
   */
  DomainUserValidatorImpl(
      DomainControllerProperties properties,
      DomainRepository domainRepository) {
    super(properties, domainRepository);
  }

  @Override
  public void doAddValidation(DomainUser domainUser) {
    //validateSamAccountNameNotExists(domainUser.getSamAccountName(), DomainUser.class);
    //validate(domainUser);
  }

  @Override
  public void doUpdateValidation(String userName, DomainUser domainUser) {
    //domainUser.setSamAccountName(userName);
    //validate(domainUser);
  }

}
