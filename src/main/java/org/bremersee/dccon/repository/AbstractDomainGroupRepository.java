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

import static org.springframework.util.ObjectUtils.isEmpty;

import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.ldaptive.LdaptiveTemplate;
import org.ldaptive.dn.Dn;

/**
 * The type AbstractDomainGroupRepository.
 *
 * @author Christian Bremer
 */
abstract class AbstractDomainGroupRepository extends AbstractDomainEntityRepository
    implements DomainGroupRepositoryConstants {

  AbstractDomainGroupRepository(
      DomainControllerProperties properties,
      LdaptiveTemplate ldapTemplate) {
    super(properties, ldapTemplate);
  }

  @Override
  Dn getDefaultOu() {
    return isEmpty(getProperties().getDefaultGroupOu())
        ? LDAP_OU_USERS
        : new Dn(getProperties().getDefaultGroupOu());
  }

  @Override
  String getObjectClassValue() {
    return LDAP_OBJECT_CLASS_GROUP;
  }

  @Override
  String[] getBinaryAttributes() {
    return LDAP_GROUP_BINARY_ATTRIBUTES;
  }

  @Override
  String[] getReturnAttributes() {
    return LDAP_GROUP_MAPPED_ATTRIBUTES;
  }

}
