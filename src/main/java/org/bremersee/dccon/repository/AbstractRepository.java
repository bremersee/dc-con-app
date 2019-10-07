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

package org.bremersee.dccon.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import org.bremersee.data.ldaptive.LdaptiveEntryMapper;
import org.bremersee.data.ldaptive.LdaptiveTemplate;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.repository.cli.CommandExecutor;
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchScope;
import org.ldaptive.io.StringValueTranscoder;
import org.springframework.util.Assert;

/**
 * The abstract repository.
 *
 * @author Christian Bremer
 */
abstract class AbstractRepository {

  private static final Object KINIT_LOG = new Object();

  private static final String KINIT_PASSWORD_FILE = "--password-file={}";

  private static final String USE_KERBEROS = "-k";

  private static final String YES = "yes";

  @Getter(AccessLevel.PACKAGE)
  private final DomainControllerProperties properties;

  @Getter(AccessLevel.PACKAGE)
  private final LdaptiveTemplate ldapTemplate;

  /**
   * Instantiates a new abstract repository.
   *
   * @param properties   the properties
   * @param ldapTemplate the ldap template
   */
  AbstractRepository(
      final DomainControllerProperties properties,
      final LdaptiveTemplate ldapTemplate) {
    Assert.notNull(properties, "Domain controller properties must not be null.");
    this.properties = properties;
    this.ldapTemplate = ldapTemplate;
  }

  /**
   * Calls linux command {@code kinit} for authentication.
   */
  void kinit() {
    synchronized (KINIT_LOG) {
      List<String> commands = new ArrayList<>();
      sudo(commands);
      commands.add(properties.getKinitBinary());
      commands.add(KINIT_PASSWORD_FILE.replace("{}", properties.getKinitPasswordFile()));
      commands.add(properties.getKinitAdministratorName());
      CommandExecutor.exec(commands, properties.getSambaToolExecDir());
    }
  }

  /**
   * Calls linux command {@code sudo}.
   *
   * @param commands the commands
   */
  void sudo(List<String> commands) {
    if (properties.isUsingSudo()) {
      commands.add(properties.getSudoBinary());
    }
  }

  /**
   * Adds the use kerberos option of the linux command {@code samba-tool} to the list of commands.
   * This requires a successful authentication with {@code kinit}, see {@link #kinit()}.
   *
   * @param commands the commands
   */
  void auth(List<String> commands) {
    commands.add(USE_KERBEROS);
    commands.add(YES);
  }

  /**
   * Checks whether the given value contains the given query.
   *
   * @param value the value
   * @param query the query
   * @return {@code true} if the value contains the query, otherwise {@code false}
   */
  boolean contains(Object value, String query) {
    if (value instanceof Collection) {
      for (Object item : (Collection) value) {
        if (contains(item, query)) {
          return true;
        }
      }
      return false;
    }
    return value != null && value.toString().toLowerCase().contains(query);
  }

  DomainGroup addAvailableMembers(DomainGroup domainGroup) {
    domainGroup.setAvailableMembers(
        findAvailableMembersOrGroup(false, domainGroup.getMembers()));
    return domainGroup;
  }

  DomainUser addAvailableGroups(DomainUser domainUser) {
    domainUser.setAvailableGroups(
        findAvailableMembersOrGroup(true, domainUser.getGroups()));
    return domainUser;
  }

  private List<String> findAvailableMembersOrGroup(
      boolean isUserName,
      Collection<String> existing) {
    final String filter =
        isUserName ? properties.getGroupFindAllFilter() : properties.getUserFindAllFilter();
    final String baseDn = isUserName ? properties.getGroupBaseDn() : properties.getUserBaseDn();
    final String attr = isUserName ? properties.getGroupRdn() : properties.getUserRdn();
    final SearchScope searchScope = isUserName
        ? properties.getGroupFindAllSearchScope()
        : properties.getUserFindAllSearchScope();
    final SearchFilter searchFilter = new SearchFilter(filter);
    final SearchRequest searchRequest = new SearchRequest();
    searchRequest.setBaseDn(baseDn);
    searchRequest.setReturnAttributes(attr);
    searchRequest.setSearchFilter(searchFilter);
    searchRequest.setSearchScope(searchScope);
    final Set<String> existingSet = new HashSet<>(existing);
    return getLdapTemplate().findAll(searchRequest).stream()
        .map(ldapEntry -> LdaptiveEntryMapper.getAttributeValue(
            ldapEntry, attr, new StringValueTranscoder(), null))
        .filter(name -> !existingSet.contains(name))
        .sorted(String::compareToIgnoreCase)
        .collect(Collectors.toList());
  }

}
