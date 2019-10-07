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
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.data.ldaptive.LdaptiveEntryMapper;
import org.bremersee.data.ldaptive.LdaptiveTemplate;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.repository.cli.CommandExecutor;
import org.bremersee.dccon.repository.cli.CommandExecutorResponse;
import org.bremersee.dccon.repository.cli.CommandExecutorResponseValidator;
import org.bremersee.dccon.repository.ldap.DomainGroupLdapMapper;
import org.bremersee.exception.ServiceException;
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * The domain group repository.
 *
 * @author Christian Bremer
 */
@Profile("ldap")
@Component("domainGroupRepository")
@Slf4j
public class DomainGroupRepositoryImpl extends AbstractRepository implements DomainGroupRepository {

  private LdaptiveEntryMapper<DomainGroup> domainGroupLdapMapper;

  /**
   * Instantiates a new domain group repository.
   *
   * @param properties   the properties
   * @param ldapTemplate the ldap template
   */
  public DomainGroupRepositoryImpl(DomainControllerProperties properties,
      LdaptiveTemplate ldapTemplate) {
    super(properties, ldapTemplate);
    domainGroupLdapMapper = new DomainGroupLdapMapper(properties);
  }

  /**
   * Sets domain group ldap mapper.
   *
   * @param domainGroupLdapMapper the domain group ldap mapper
   */
  @SuppressWarnings("unused")
  public void setDomainGroupLdapMapper(
      LdaptiveEntryMapper<DomainGroup> domainGroupLdapMapper) {
    if (domainGroupLdapMapper != null) {
      this.domainGroupLdapMapper = domainGroupLdapMapper;
    }
  }

  @Override
  public Stream<DomainGroup> findAll(String query) {
    final SearchRequest searchRequest = new SearchRequest(
        getProperties().getGroupBaseDn(),
        new SearchFilter(getProperties().getGroupFindAllFilter()));
    searchRequest.setSearchScope(getProperties().getGroupFindAllSearchScope());
    if (query == null || query.trim().length() == 0) {
      return getLdapTemplate().findAll(searchRequest, domainGroupLdapMapper);
    } else {
      return getLdapTemplate().findAll(searchRequest, domainGroupLdapMapper)
          .filter(domainGroup -> this.isQueryResult(domainGroup, query.trim().toLowerCase()));
    }
  }

  private boolean isQueryResult(DomainGroup domainGroup, String query) {
    return query != null && query.length() > 2 && domainGroup != null
        && (contains(domainGroup.getName(), query)
        || contains(domainGroup.getDescription(), query)
        || contains(domainGroup.getMembers(), query));
  }

  @Override
  public Optional<DomainGroup> findOne(@NotNull String groupName, Boolean addAvailableMembers) {
    final SearchFilter searchFilter = new SearchFilter(getProperties().getGroupFindOneFilter());
    searchFilter.setParameter(0, groupName);
    final SearchRequest searchRequest = new SearchRequest(
        getProperties().getGroupBaseDn(),
        searchFilter);
    searchRequest.setSearchScope(getProperties().getGroupFindOneSearchScope());
    return getLdapTemplate().findOne(searchRequest, domainGroupLdapMapper)
        .map(domainGroup -> Boolean.TRUE.equals(addAvailableMembers)
            ? addAvailableMembers(domainGroup)
            : domainGroup);
  }

  @Override
  public boolean exists(@NotNull String groupName) {
    return getLdapTemplate()
        .exists(DomainGroup.builder().name(groupName).build(), domainGroupLdapMapper);
  }

  @Override
  public DomainGroup save(@NotNull DomainGroup domainGroup) {
    if (!exists(domainGroup.getName())) {
      kinit();
      final List<String> commands = new ArrayList<>();
      sudo(commands);
      commands.add(getProperties().getSambaToolBinary());
      commands.add("group");
      commands.add("add");
      commands.add(domainGroup.getName());
      auth(commands);
      CommandExecutor.exec(
          commands,
          null,
          getProperties().getSambaToolExecDir(),
          (CommandExecutorResponseValidator) response -> {
            if (!exists(domainGroup.getName())) {
              throw ServiceException.internalServerError("msg=[Saving group failed.] groupName=["
                      + domainGroup.getName() + "] "
                      + CommandExecutorResponse.toExceptionMessage(response),
                  "org.bremersee:dc-con-app:7729c3c7-aeff-49f2-9243-dd5aee4b023a");
            }
          });
    }
    return getLdapTemplate().save(domainGroup, domainGroupLdapMapper);
  }

  @Override
  public boolean delete(@NotNull String groupName) {

    if (exists(groupName)) {
      kinit();
      final List<String> commands = new ArrayList<>();
      sudo(commands);
      commands.add(getProperties().getSambaToolBinary());
      commands.add("group");
      commands.add("delete");
      commands.add(groupName);
      auth(commands);
      CommandExecutor.exec(
          commands,
          null,
          getProperties().getSambaToolExecDir(),
          (CommandExecutorResponseValidator) response -> {
            if (exists(groupName)) {
              throw ServiceException.internalServerError(
                  "msg=[Deleting group failed.] groupName=[" + groupName + "] "
                      + CommandExecutorResponse.toExceptionMessage(response),
                  "org.bremersee:dc-con-app:28f610a5-1679-47d9-8f90-2a4d75882d52");
            }
          });
      return true;
    }
    return false;
  }


}
