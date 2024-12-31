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
import org.springframework.util.StringUtils;

/**
 * The default domain user validator.
 */
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
    validateSamAccountNameNotExists(domainUser.getSamAccountName(), DomainUser.class);
    validate(domainUser);
  }

  @Override
  public void doUpdateValidation(String userName, DomainUser domainUser) {
    domainUser.setSamAccountName(userName);
    validate(domainUser);
  }

  private void validate(DomainUser domainUser) {
    // Display name
    final StringBuilder displayNameBuilder = new StringBuilder();
    if (StringUtils.hasText(domainUser.getFirstName())) {
      displayNameBuilder.append(domainUser.getFirstName());
      if (StringUtils.hasText(domainUser.getLastName())) {
        displayNameBuilder.append(' ');
      }
    }
    if (StringUtils.hasText(domainUser.getLastName())) {
      displayNameBuilder.append(domainUser.getLastName());
    }
    final String displayName = StringUtils.hasText(domainUser.getDisplayName())
        ? domainUser.getDisplayName()
        : !displayNameBuilder.isEmpty() ? displayNameBuilder.toString() : null;
    domainUser.setDisplayName(displayName);

    // TODO depends on rf...
    // Login shell
    if (StringUtils.hasText(getProperties().getLoginShell())) {
      domainUser.setLoginShell(getProperties().getLoginShell());
    } else {
      domainUser.setLoginShell(null);
    }

    // Unix home
    if (StringUtils.hasText(getProperties().getUnixHomeDirTemplate())) {
      domainUser.setUnixHomeDirectory(getProperties()
          .getUnixHomeDirTemplate().replace("{}", domainUser.getSamAccountName()));
    } else {
      domainUser.setUnixHomeDirectory(null);
    }

    // Home directory/share
    if (StringUtils.hasText(getProperties().getHomeDirectoryTemplate())) {
      domainUser.setHomeDirectory(getProperties()
          .getHomeDirectoryTemplate().replace("{}", domainUser.getSamAccountName()));
    } else {
      domainUser.setHomeDirectory(null);
    }
  }
}
