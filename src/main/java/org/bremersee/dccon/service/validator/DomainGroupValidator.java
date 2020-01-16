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

import java.util.stream.Collectors;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.repository.DomainGroupRepository;
import org.bremersee.dccon.repository.DomainUserRepository;

/**
 * The domain group validator interface.
 *
 * @author Christian Bremer
 */
public interface DomainGroupValidator {

  /**
   * Do add validation.
   *
   * @param domainGroup the domain group
   */
  void doAddValidation(DomainGroup domainGroup);

  /**
   * Do update validation.
   *
   * @param groupName the group name
   * @param domainGroup the domain group
   */
  void doUpdateValidation(String groupName, DomainGroup domainGroup);

  /**
   * Default domain group validator.
   *
   * @param properties the properties
   * @param groupRepository the group repository
   * @param userRepository the user repository
   * @return the domain group validator
   */
  static DomainGroupValidator defaultValidator(
      DomainControllerProperties properties,
      DomainGroupRepository groupRepository,
      DomainUserRepository userRepository) {
    return new Default(properties, groupRepository, userRepository);
  }

  /**
   * The default domain group validator.
   */
  class Default extends AbstractDomainEntityValidator implements DomainGroupValidator {

    /**
     * Instantiates a new default domain group validator.
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
    public void doAddValidation(DomainGroup domainGroup) {
      validateNameNotExists(domainGroup.getName(), DomainGroup.class);
      domainGroup.setMembers(domainGroup.getMembers().stream()
          .filter(name -> getUserRepository().exists(name) || getGroupRepository().exists(name))
          .collect(Collectors.toList()));
    }

    @Override
    public void doUpdateValidation(String groupName, DomainGroup domainGroup) {
      domainGroup.setName(groupName);
      domainGroup.setMembers(domainGroup.getMembers().stream()
          .filter(name -> getUserRepository().exists(name) || getGroupRepository().exists(name))
          .collect(Collectors.toList()));
    }
  }

}
