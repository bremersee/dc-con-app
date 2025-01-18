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

import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.SamAccount;
import org.bremersee.ldaptive.LdaptiveTemplate;
import org.ldaptive.LdapEntry;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchScope;
import org.ldaptive.filter.EqualityFilter;

/**
 * The type AbstractSamAccountRepository.
 *
 * @author Christian Bremer
 */
@Slf4j
abstract class AbstractSamAccountRepository extends AbstractOrganizedEntryRepository {

  /**
   * Instantiates a new abstract repository.
   *
   * @param properties the properties
   * @param ldapTemplate the ldap template
   */
  AbstractSamAccountRepository(DomainControllerProperties properties,
      LdaptiveTemplate ldapTemplate) {
    super(properties, ldapTemplate);
  }

  boolean samAccountExists(SamAccount samAccount) {
    return findDnOfSamAccount(samAccount).isPresent();
  }

  boolean samAccountNameExists(String samAccountName) {
    return findDnOfSamAccountName(samAccountName).isPresent();
  }

  Optional<String> findDnOfSamAccount(SamAccount samAccount) {
    if (isEmpty(samAccount)) {
      return Optional.empty();
    }
    return findDnOfSamAccountName(samAccount.getSamAccountName());
  }

  Optional<String> findDnOfSamAccountName(String samAccountName) {
    log.debug("findDnOfSamAccountName({})", samAccountName);
    if (isEmpty(samAccountName)) {
      log.debug("Dn of '{}' does not exist", samAccountName);
      return Optional.empty();
    }
    SearchRequest searchRequest = SearchRequest.builder()
        .dn(getProperties().getBaseDn().format())
        .filter(new EqualityFilter(RepositoryConstants.LDAP_SAM_ACCOUNT_NAME, samAccountName))
        .scope(SearchScope.SUBTREE)
        .returnAttributes(RepositoryConstants.LDAP_DN)
        .sizeLimit(1)
        .build();
    log.debug("findDnOfSamAccountName, searchRequest = {}", searchRequest);
    return getLdapTemplate().findOne(searchRequest)
        .map(LdapEntry::getDn)
        .filter(getIgnoredDnFilter());
  }

}
