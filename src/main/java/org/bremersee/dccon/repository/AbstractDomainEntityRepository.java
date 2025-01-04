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

import static java.util.Objects.requireNonNullElseGet;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.Optional;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.CommonAttributes;
import org.bremersee.exception.ServiceException;
import org.bremersee.ldaptive.LdaptiveException;
import org.bremersee.ldaptive.LdaptiveTemplate;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
import org.ldaptive.filter.AndFilter;
import org.ldaptive.filter.EqualityFilter;
import org.ldaptive.filter.Filter;

/**
 * The type AbstractLdapRepository.
 *
 * @author Christian Bremer
 */
abstract class AbstractDomainEntityRepository extends AbstractRepository {

  /**
   * Instantiates a new abstract repository.
   *
   * @param properties the properties
   * @param ldapTemplate the ldap template
   */
  AbstractDomainEntityRepository(DomainControllerProperties properties,
      LdaptiveTemplate ldapTemplate) {
    super(properties, ldapTemplate);
  }

  abstract String getObjectClassValue();

  String getUniqueNameAttributeName() {
    return LDAP_SAM_ACCOUNT_NAME;
  }

  abstract String[] getBinaryAttributes();

  abstract String[] getReturnAttributes();

  Filter objectClassFilter() {
    return new EqualityFilter(LDAP_OBJECT_CLASS, getObjectClassValue());
  }

  Filter findOneFilter(String uniqueName) {
    return new AndFilter(
        objectClassFilter(),
        new EqualityFilter(getUniqueNameAttributeName(), uniqueName));
  }

  SearchRequest searchOneRequest(
      String uniqueName,
      String... returnAttributes) {
    return searchOneRequest(uniqueName, null, null, returnAttributes);
  }

  SearchRequest searchOneRequest(
      String uniqueName,
      Dn ouRdn,
      SearchScope scope,
      String... returnAttributes) {
    return searchOneRequest(uniqueName, ouRdn, null, scope, returnAttributes);
  }

  SearchRequest searchOneRequest(
      String uniqueName,
      Dn ouRdn,
      Filter filter,
      SearchScope scope,
      String... returnAttributes) {

    if (isDn(uniqueName)) {
      return SearchRequest.builder()
          .dn(uniqueName)
          .filter(objectClassFilter())
          .scope(SearchScope.OBJECT)
          .binaryAttributes(getBinaryAttributes())
          .returnAttributes(isEmpty(returnAttributes) ? getReturnAttributes() : returnAttributes)
          .sizeLimit(1)
          .build();
    }
    return SearchRequest.builder()
        .dn(getBaseDn(ouRdn).format())
        .filter(requireNonNullElseGet(filter, () -> findOneFilter(uniqueName)))
        .scope(Optional.ofNullable(scope)
            .filter(searchScope -> !isEmpty(ouRdn))
            .orElse(SearchScope.SUBTREE))
        .binaryAttributes(getBinaryAttributes())
        .returnAttributes(isEmpty(returnAttributes) ? getReturnAttributes() : returnAttributes)
        .sizeLimit(1)
        .build();
  }

  SearchRequest searchAllRequest(
      Dn ouRdn,
      Filter filter,
      SearchScope scope,
      String... returnAttributes) {
    return SearchRequest.builder()
        .dn(getBaseDn(ouRdn).format())
        .filter(filter)
        .scope(Optional.ofNullable(scope)
            .filter(searchScope -> !isEmpty(ouRdn))
            .orElse(SearchScope.SUBTREE))
        .binaryAttributes(getBinaryAttributes())
        .returnAttributes(isEmpty(returnAttributes) ? getReturnAttributes() : returnAttributes)
        .build();
  }

}
