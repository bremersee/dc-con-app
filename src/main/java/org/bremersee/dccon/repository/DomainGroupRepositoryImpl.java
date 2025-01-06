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

package org.bremersee.dccon.repository;

import static java.util.Objects.isNull;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.model.Sid;
import org.bremersee.dccon.repository.automock.MockComponent;
import org.bremersee.dccon.repository.automock.ProfileRequired;
import org.bremersee.dccon.repository.cli.CommandExecutor;
import org.bremersee.dccon.repository.cli.CommandExecutorResponse;
import org.bremersee.exception.ServiceException;
import org.bremersee.ldaptive.LdaptiveEntryMapper;
import org.bremersee.ldaptive.LdaptiveTemplate;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
import org.ldaptive.filter.AndFilter;
import org.ldaptive.filter.EqualityFilter;
import org.ldaptive.filter.Filter;
import org.ldaptive.filter.OrFilter;
import org.ldaptive.filter.SubstringFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * The domain group repository.
 *
 * @author Christian Bremer
 */
@Primary
@Component("domainGroupRepository")
@ProfileRequired("ldap")
@MockComponent(value = DomainUserRepositoryMock.class, methodsOf = DomainGroupRepository.class)
@Slf4j
public class DomainGroupRepositoryImpl extends AbstractDomainGroupRepository
    implements DomainGroupRepository {

  private final LdaptiveEntryMapper<DomainGroup> domainGroupLdapMapper;

  private final Dn builtinDn;

  /**
   * Instantiates a new domain group repository.
   *
   * @param properties the properties
   * @param ldapTemplateProvider the ldap template provider
   */
  public DomainGroupRepositoryImpl(
      DomainControllerProperties properties,
      ObjectProvider<LdaptiveTemplate> ldapTemplateProvider,
      LdaptiveEntryMapper<DomainGroup> domainGroupLdapMapper,
      DomainRepository domainRepository) {
    super(properties, ldapTemplateProvider.getIfAvailable(), domainRepository);
    this.domainGroupLdapMapper = domainGroupLdapMapper;
    builtinDn = properties.getBaseDn(new Dn("CN=Builtin"));
  }

  Filter getFindAllFilter(String query) {
    //noinspection DuplicatedCode
    Filter objectClassFilter = new EqualityFilter(LDAP_OBJECT_CLASS, LDAP_OBJECT_CLASS_GROUP);
    if (isNull(query) || query.length() <= 2) {
      return objectClassFilter;
    }
    Filter orFilter = new OrFilter(
        new SubstringFilter(LDAP_SAM_ACCOUNT_NAME, null, null, query),
        new SubstringFilter(LDAP_DESCRIPTION, null, null, query),
        new SubstringFilter(LDAP_MAIL, null, null, query),
        new EqualityFilter(LDAP_GID_NUMBER, query)
    );
    return new AndFilter(objectClassFilter, orFilter);
  }

  @Override
  public Stream<DomainGroup> findAll(String query, Dn ou, SearchScope searchScope) {
    log.debug("findAll({}, {}, {})", query, ou, searchScope);
    SearchRequest searchRequest = searchAllRequest(
        ou,
        getFindAllFilter(query),
        searchScope,
        getReturnAttributes());
    return getLdapTemplate()
        .findAll(searchRequest, domainGroupLdapMapper)
        .filter(getNoBuiltinObjectFilter())
        .peek(group -> log.debug("Found group: {}", group.getDistinguishedName()));
  }

  @Override
  public Optional<DomainGroup> findOne(String groupName, Dn ou, SearchScope searchScope) {
    SearchRequest searchRequest = searchOneRequest(groupName, ou, searchScope);
    return getLdapTemplate()
        .findOne(searchRequest, domainGroupLdapMapper)
        .filter(getNoBuiltinObjectFilter());
  }

  @ProfileRequired({"cli", "ldap"})
  @Override
  public DomainGroup add(DomainGroup domainGroup, Dn ou) {
    if (getDomainRepository().samAccountNameExists(domainGroup.getSamAccountName())) {
      throw ServiceException.alreadyExistsWithErrorCode(
          DomainUser.class.getSimpleName(),
          domainGroup.getSamAccountName(),
          EC_SAM_ACCOUNT_ALREADY_EXISTS);
    }
    String dn = doAdd(domainGroup, ou);
    domainGroup.setDistinguishedName(dn);
    return getLdapTemplate().save(domainGroup, domainGroupLdapMapper);
  }

  /**
   * Add group.
   *
   * @param domainGroup the domain group
   */
  String doAdd(DomainGroup domainGroup, Dn ou) {
    kinit();
    final List<String> commands = new ArrayList<>();
    ssh(commands);
    sudo(commands);
    commands.add(getProperties().getSambaToolBinary());
    commands.add("group");
    commands.add("add");
    commands.add(
        quote(domainGroup.getSamAccountName())); // TODO what happens if name contains a space?
    if (!isEmpty(ou) && !ou.isEmpty()) {
      commands.add("--groupou=" + quote(validateOu(ou).format()));
    }
    //auth(commands);
    return CommandExecutor.exec(
        commands,
        null,
        getProperties().getSambaToolExecDir(),
        response -> getDomainRepository().findDnOfSamAccount(domainGroup)
            .orElseThrow(() -> ServiceException
                .internalServerError(String.format("Adding group '%s' failed: %s",
                        domainGroup.getSamAccountName(),
                        CommandExecutorResponse.toExceptionMessage(response)),
                    EC_ADDING_GROUP_FAILED)));
  }

  public DomainGroup update(DomainGroup domainGroup) {
    return getDomainRepository().findDnOfSamAccount(domainGroup)
        .map(dn -> validateDn(domainGroup, dn))
        .map(dn -> getLdapTemplate().save(domainGroup, domainGroupLdapMapper))
        .orElseThrow(() -> ServiceException.notFoundWithErrorCode(
            DomainGroup.class.getSimpleName(),
            domainGroup.getSamAccountName(),
            EC_SAM_ACCOUNT_NOT_FOUND));
  }

  public DomainGroup update(String groupName, DomainGroup domainGroup, Dn newOu) {
    // TODO
    return null;
  }

  // TODO
  public boolean hasAllNisAttributes(DomainGroup domainUser) {
    return !isEmpty(domainUser)
        && !isEmpty(getNisDomain(domainUser))
        && !isEmpty(domainUser.getGidNumber());
  }

  @ProfileRequired({"cli", "ldap"})
  @Override
  public boolean delete(String groupName) {
    log.debug("delete({})", groupName);
    return findOne(groupName, null, null)
        .filter(group -> Optional.ofNullable(group.getSid())
            .map(Sid::getSystemEntity)
            .orElse(false))
        .map(group -> doDelete(group.getSamAccountName()))
        .orElse(false);
  }

  /**
   * Delete group.
   *
   * @param groupName the group name
   */
  boolean doDelete(String groupName) {
    kinit();
    final List<String> commands = new ArrayList<>();
    ssh(commands);
    sudo(commands);
    commands.add(getProperties().getSambaToolBinary());
    commands.add("group");
    commands.add("delete");
    commands.add(quote(groupName));
    auth(commands);
    return CommandExecutor.exec(
        commands,
        null,
        getProperties().getSambaToolExecDir(),
        response -> {
          if (getDomainRepository().samAccountNameExists(groupName)) {
            throw ServiceException.internalServerError(
                String.format("Deleting group '%s' failed: %s", groupName,
                    CommandExecutorResponse.toExceptionMessage(response)),
                EC_DELETING_GROUP_FAILED);
          }
          return true;
        });
  }

}
