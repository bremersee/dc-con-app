/*
 * Copyright 2025 the original author or authors.
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
import org.bremersee.dccon.model.NisDomainMember;
import org.bremersee.ldaptive.LdaptiveException;
import org.bremersee.ldaptive.LdaptiveTemplate;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
import org.ldaptive.filter.AndFilter;
import org.ldaptive.filter.EqualityFilter;
import org.ldaptive.filter.Filter;

/**
 * The type AbstractOrganizedEntryRepository.
 *
 * @author Christian Bremer
 */
abstract class AbstractOrganizedEntryRepository extends AbstractRepository {

  /**
   * Instantiates a new abstract repository.
   *
   * @param properties the properties
   * @param ldapTemplate the ldap template
   */
  AbstractOrganizedEntryRepository(
      DomainControllerProperties properties,
      LdaptiveTemplate ldapTemplate) {
    super(properties, ldapTemplate);
  }

  abstract Dn getDefaultOu();

  Dn validateOu(Dn ou) {
    Dn ouDn = isEmpty(ou) || ou.isEmpty() ? getDefaultOu() : ou;
    if (isEmpty(ouDn) || ouDn.isEmpty()) {
      throw LdaptiveException.badRequest(
          "Organizational unit cannot be empty.", EC_EMPTY_OU_RDN);
    }
    Dn dn = getProperties().getBaseDn(ouDn);
    if (!isEmpty(getLdapTemplate()) && !getLdapTemplate().exists(dn.format())) {
      throw LdaptiveException.badRequest(
          String.format("Organizational unit '%s' does not exist.", ouDn.format()),
          EC_OU_NOT_FOUND);
    }
    return ouDn;
  }

  String getNisDomain(NisDomainMember nisDomainMember) {
    return !isEmpty(nisDomainMember) && !isEmpty(nisDomainMember.getNisDomain())
        ? nisDomainMember.getNisDomain()
        : getProperties().getDomain().getDefaultNisDomain();
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

    if (getProperties().isDn(uniqueName)) {
      return SearchRequest.builder()
          .dn(uniqueName)
          .filter(objectClassFilter())
          .scope(SearchScope.OBJECT)
          .binaryAttributes(getBinaryAttributes())
          .returnAttributes(isEmpty(returnAttributes) ? getReturnAttributes() : returnAttributes)
          .sizeLimit(1)
          .build();
    }
    Dn ouDn = getProperties().getBaseDn(ouRdn);
    return SearchRequest.builder()
        .dn(ouDn.format())
        .filter(requireNonNullElseGet(filter, () -> findOneFilter(uniqueName)))
        .scope(Optional.ofNullable(scope)
            .filter(searchScope -> !ouDn.isSame(getProperties().getBaseDn()))
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
    Dn ouDn = getProperties().getBaseDn(ouRdn);
    return SearchRequest.builder()
        .dn(ouDn.format())
        .filter(filter)
        .scope(Optional.ofNullable(scope)
            .filter(searchScope -> !ouDn.isSame(getProperties().getBaseDn()))
            .orElse(SearchScope.SUBTREE))
        .binaryAttributes(getBinaryAttributes())
        .returnAttributes(isEmpty(returnAttributes) ? getReturnAttributes() : returnAttributes)
        .build();
  }

}
