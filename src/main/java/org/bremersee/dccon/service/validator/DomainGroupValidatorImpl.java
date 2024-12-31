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

import java.util.stream.Collectors;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.repository.DomainRepository;
import org.bremersee.dccon.repository.RepositoryConstants;
import org.springframework.stereotype.Component;

/**
 * The default domain group validator.
 */
@Component("domainGroupValidator")
public class DomainGroupValidatorImpl extends AbstractDomainEntityValidator
    implements DomainGroupValidator {

  /**
   * Instantiates a new default domain group validator.
   *
   * @param properties the properties
   */
  DomainGroupValidatorImpl(
      DomainControllerProperties properties,
      DomainRepository domainRepository) {
    super(properties, domainRepository);
  }

  @Override
  public void doAddValidation(DomainGroup domainGroup) {
    validateSamAccountNameNotExists(domainGroup.getSamAccountName(), DomainGroup.class);
    domainGroup.setMembers(domainGroup.getMembers().stream()
        .filter(name -> getDomainRepository().dnExistsWithAnyObjectClass(name,
            RepositoryConstants.LDAP_OBJECT_CLASS_USER,
            RepositoryConstants.LDAP_OBJECT_CLASS_GROUP,
            RepositoryConstants.LDAP_OBJECT_CLASS_COMPUTER))
        .distinct()
        .collect(Collectors.toList()));
  }

  @Override
  public void doUpdateValidation(String groupName, DomainGroup domainGroup) {
    domainGroup.setSamAccountName(groupName);
    domainGroup.setMembers(domainGroup.getMembers().stream()
        .filter(name -> getDomainRepository().dnExistsWithAnyObjectClass(name,
            RepositoryConstants.LDAP_OBJECT_CLASS_USER,
            RepositoryConstants.LDAP_OBJECT_CLASS_GROUP,
            RepositoryConstants.LDAP_OBJECT_CLASS_COMPUTER))
        .distinct()
        .collect(Collectors.toList()));
  }
}
