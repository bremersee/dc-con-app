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

import java.util.stream.Collectors;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.repository.DomainGroupRepository;
import org.bremersee.dccon.repository.DomainUserRepository;
import org.springframework.util.StringUtils;

/**
 * The domain user validator interface.
 *
 * @author Christian Bremer
 */
public interface DomainUserValidator {

  /**
   * Do add validation.
   *
   * @param domainUser the domain user
   */
  void doAddValidation(DomainUser domainUser);

  /**
   * Do update validation.
   *
   * @param userName the user name
   * @param domainUser the domain user
   */
  void doUpdateValidation(String userName, DomainUser domainUser);

  /**
   * Default domain user validator.
   *
   * @param properties the properties
   * @param groupRepository the group repository
   * @param userRepository the user repository
   * @return the domain user validator
   */
  static DomainUserValidator defaultValidator(
      DomainControllerProperties properties,
      DomainGroupRepository groupRepository,
      DomainUserRepository userRepository) {
    return new Default(properties, groupRepository, userRepository);
  }

  /**
   * The default domain user validator.
   */
  class Default extends AbstractDomainEntityValidator implements DomainUserValidator {

    /**
     * Instantiates a new default domain user validator.
     *
     * @param properties the properties
     * @param groupRepository the group repository
     * @param userRepository the user repository
     */
    Default(DomainControllerProperties properties,
        DomainGroupRepository groupRepository,
        DomainUserRepository userRepository) {
      super(properties, groupRepository, userRepository);
    }

    @Override
    public void doAddValidation(DomainUser domainUser) {
      validateNameNotExists(domainUser.getUserName(), DomainUser.class);
      validate(domainUser);
    }

    @Override
    public void doUpdateValidation(String userName, DomainUser domainUser) {
      domainUser.setUserName(userName);
      validate(domainUser);
    }

    private void validate(DomainUser domainUser) {
      domainUser.setGroups(domainUser.getGroups().stream()
          .filter(name -> getGroupRepository().exists(name))
          .collect(Collectors.toList()));

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
          : displayNameBuilder.length() > 0 ? displayNameBuilder.toString() : null;
      domainUser.setDisplayName(displayName);

      // Login shell
      if (StringUtils.hasText(getProperties().getLoginShell())) {
        domainUser.setLoginShell(getProperties().getLoginShell());
      } else {
        domainUser.setLoginShell(null);
      }

      // Unix home
      if (StringUtils.hasText(getProperties().getUnixHomeDirTemplate())) {
        domainUser.setUnixHomeDirectory(getProperties()
            .getUnixHomeDirTemplate().replace("{}", domainUser.getUserName()));
      } else {
        domainUser.setUnixHomeDirectory(null);
      }

      // Home directory/share
      if (StringUtils.hasText(getProperties().getHomeDirectoryTemplate())) {
        domainUser.setHomeDirectory(getProperties()
            .getHomeDirectoryTemplate().replace("{}", domainUser.getUserName()));
      } else {
        domainUser.setHomeDirectory(null);
      }
    }
  }

}
