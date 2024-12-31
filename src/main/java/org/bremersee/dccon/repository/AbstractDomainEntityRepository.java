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

  abstract Dn getDefaultOu();

  Dn getBaseDn(Dn ouRdn) {
    if (isEmpty(ouRdn) || ouRdn.isEmpty()) {
      return getBaseDn();
    }
    Dn dn = new Dn(ouRdn.getRDns());
    if (dn.isSame(getBaseDn()) || getBaseDn().isAncestor(dn)) {
      return dn;
    }
    dn.add(getBaseDn());
    return dn;
  }

  Dn validateOu(Dn ouRdn) {
    Dn ou = isEmpty(ouRdn) || ouRdn.isEmpty() ? getDefaultOu() : ouRdn;
    if (isEmpty(ou) || ou.isEmpty()) {
      throw LdaptiveException.badRequest(
          "Organizational unit cannot be empty.", EC_EMPTY_OU_RDN);
    }
    Dn dn = new Dn(ou.getRDns());
    if (!getBaseDn().isAncestor(ou)) {
      dn.add(getBaseDn());
    }
    if (!getLdapTemplate().exists(dn.format())) {
      throw LdaptiveException.badRequest(
          String.format("Organizational unit '%s' does not exist.", ou.format()), EC_OU_NOT_FOUND);
    }
    return ou;
  }

  String validateDn(CommonAttributes ldapEntry, String dn) {
    if (isEmpty(ldapEntry.getDistinguishedName())) {
      ldapEntry.setDistinguishedName(dn);
      return dn;
    }
    try {
      if (new Dn(dn).isSame(new Dn(ldapEntry.getDistinguishedName()))) {
        return dn;
      }

    } catch (RuntimeException e) {
      // ignored
    }
    throw ServiceException.badRequest(String.format("Distinguished name of object '%s' is not "
            + "the same distinguished name of the ldap entry '%s'.",
        ldapEntry.getDistinguishedName(), dn), EC_ILLEGAL_DN);
  }

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
