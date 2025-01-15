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
import static java.util.Objects.requireNonNullElse;
import static org.bremersee.dccon.repository.DomainUserRepositoryConstants.LDAP_USER_DISPLAY_NAME;
import static org.bremersee.dccon.repository.DomainUserRepositoryConstants.LDAP_USER_GIVEN_NAME;
import static org.bremersee.dccon.repository.DomainUserRepositoryConstants.LDAP_USER_SN;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.converter.TreeSearchScopeConverter;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.model.DomainGroupMember;
import org.bremersee.dccon.model.DomainGroupType;
import org.bremersee.dccon.model.DomainGroupTypeContainer;
import org.bremersee.dccon.model.SamAccount;
import org.bremersee.dccon.model.TreeSearchScope;
import org.bremersee.dccon.repository.automock.MockComponent;
import org.bremersee.dccon.repository.automock.ProfileRequired;
import org.bremersee.dccon.repository.cli.CommandExecutor;
import org.bremersee.dccon.repository.cli.CommandExecutorResponse;
import org.bremersee.dccon.repository.cli.CommandExecutorResponseValidator;
import org.bremersee.dccon.repository.mapper.DomainGroupLdapMapper;
import org.bremersee.exception.ServiceException;
import org.bremersee.ldaptive.LdaptiveTemplate;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
import org.ldaptive.dn.NameValue;
import org.ldaptive.dn.RDn;
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
@MockComponent(value = DomainGroupRepositoryMock.class, methodsOf = DomainGroupRepository.class)
@Slf4j
public class DomainGroupRepositoryImpl extends AbstractDomainGroupRepository
    implements DomainGroupRepository {

  private final DomainGroupLdapMapper domainGroupLdapMapper;
  private final DomainRepository domainRepository;

  /**
   * Instantiates a new domain group repository.
   *
   * @param properties the properties
   * @param ldapTemplateProvider the ldap template provider
   */
  public DomainGroupRepositoryImpl(
      DomainControllerProperties properties,
      ObjectProvider<LdaptiveTemplate> ldapTemplateProvider,
      DomainGroupLdapMapper domainGroupLdapMapper,
      DomainRepository domainRepository) {
    super(properties, ldapTemplateProvider.getIfAvailable(), domainRepository);
    this.domainGroupLdapMapper = domainGroupLdapMapper;
    this.domainRepository = domainRepository;
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
  public Stream<DomainGroup> findAll(String query, Dn ou, TreeSearchScope searchScope) {
    log.debug("findAll({}, {}, {})", query, ou, searchScope);
    SearchScope scope = TreeSearchScopeConverter.toSearchScope(searchScope);
    SearchRequest searchRequest = searchAllRequest(
        ou,
        getFindAllFilter(query),
        scope,
        getReturnAttributes());
    return getLdapTemplate()
        .findAll(searchRequest, domainGroupLdapMapper)
        .filter(getIgnoredObjectFilter(ou, scope))
        .peek(group -> log.debug("Found group: {}", group.getDistinguishedName()));
  }

  @Override
  public Stream<DomainGroup> resolveMemberships(
      String samAccountName, Dn ou, TreeSearchScope searchScope) {
    log.debug("resolveMemberships({}, {}, {})", samAccountName, ou, searchScope);
    Set<Dn> groupDns = new HashSet<>();
    return getMemberships(samAccountName, ou, searchScope)
        .filter(group -> !groupDns.contains(new Dn(group.getDistinguishedName())))
        .peek(group -> groupDns.add(new Dn(group.getDistinguishedName())))
        .flatMap(group -> Stream
            .concat(Stream.of(group), resolveMemberships(group.getMemberships(), groupDns)));
  }

  private Stream<DomainGroup> resolveMemberships(List<String> memberOf, Set<Dn> groupDns) {
    return memberOf.stream()
        .filter(dn -> !groupDns.contains(new Dn(dn)))
        .flatMap(dn -> findOne(dn, null, null).stream())
        .peek(group -> groupDns.add(new Dn(group.getDistinguishedName())))
        .flatMap(nextGroup -> Stream
            .concat(Stream.of(nextGroup),
                resolveMemberships(nextGroup.getMemberships(), groupDns)));
  }

  /*
  public Stream<DomainGroup> resolveMemberships(
      String samAccountName, Dn ou, TreeSearchScope searchScope) {
    log.debug("resolveMemberships({}, {}, {})", samAccountName, ou, searchScope);

    List<DomainGroup> groups = getMemberships(samAccountName, ou, searchScope)
        .collect(Collectors.toCollection(ArrayList::new));
    Set<String> groupDns = groups.stream()
        .map(DomainGroup::getDistinguishedName)
        .map(Dn::new)
        .map(Dn::format)
        .collect(Collectors.toCollection(HashSet::new));
    for (DomainGroup group : groups) {
      resolveMemberships(group, groups, groupDns);
    }
    return groups.stream();
  }
  private void resolveMemberships(
      DomainGroup group, List<DomainGroup> groups, Set<String> groupDns) {
    if (isEmpty(group)) {
      return;
    }
    String groupDn = new Dn(group.getDistinguishedName()).format();
    if (!groupDns.contains(groupDn)) {
      groupDns.add(groupDn);
      groups.add(group);
      for (String dn : group.getMemberships()) {
        DomainGroup nextGroup = findOne(dn, null, null).orElse(null);
        resolveMemberships(nextGroup, groups, groupDns);
      }
    }
  }
  */

  @Override
  public Stream<DomainGroup> getMemberships(
      String samAccountName, Dn ou, TreeSearchScope searchScope) {

    log.debug("getMemberships({}, {}, {})", samAccountName, ou, searchScope);
    SamAccount samAccount = findSamAccount(samAccountName, ou, searchScope)
        .orElseThrow(() -> ServiceException.notFoundWithErrorCode(
            SamAccount.class.getSimpleName(), samAccountName, EC_SAM_ACCOUNT_NOT_FOUND));
    Stream<DomainGroup> groups = samAccount.getMemberships().stream()
        .flatMap(dn -> findOne(dn, null, null).stream())
        .sorted();
    Optional<DomainGroup> primaryGroup = findOneByPrimaryGroupId(samAccount.getPrimaryGroupId());
    if (primaryGroup.isPresent() && equals(primaryGroup.get(), samAccount)) {
      return groups;
    }
    return Stream.concat(primaryGroup.stream(), groups);
  }

  // TODO move to object? use Dn in model! Ah, nee, dn to string ist scheiße -> Override!
  private boolean equals(SamAccount samAccount1, SamAccount samAccount2) {
    boolean result = Objects.equals(samAccount1, samAccount2);
    if (result) {
      return true;
    }
    result = Objects.equals(samAccount1.getSamAccountName(), samAccount2.getSamAccountName());
    if (result) {
      return true;
    }
    return Objects.equals(
        samAccount1.getSamAccountName().toLowerCase(),
        samAccount2.getSamAccountName().toLowerCase());
  }

  @Override
  public Stream<DomainGroupMember> findPossibleMembers(
      String groupName, Dn ou, TreeSearchScope searchScope) {
    log.debug("findPossibleMembers({}, {}, {})", groupName, ou, searchScope);
    return findPossibleMembers(groupName, ou, searchScope, null);
  }

  @Override
  public Stream<DomainGroupMember> queryPossibleMembers(String groupName, Dn ou,
      TreeSearchScope searchScope, String query) {
    log.debug("queryPossibleMembers({}, {}, {}, {})", groupName, ou, searchScope, query);
    if (isEmpty(query) || query.length() <= 2) {
      return Stream.empty();
    }
    return findPossibleMembers(groupName, ou, searchScope, query);
  }

  Stream<DomainGroupMember> findPossibleMembers(
      String groupName,
      Dn ou,
      TreeSearchScope searchScope,
      String query) {

    DomainGroup group = findOne(groupName, ou, searchScope)
        .orElseThrow(() -> ServiceException.notFoundWithErrorCode(
            DomainGroup.class.getSimpleName(), groupName, EC_SAM_ACCOUNT_NOT_FOUND));
    Set<String> memberDns = group.getMembers().stream()
        .map(Dn::new)
        .map(Dn::format)
        .collect(Collectors.toSet());
    if (isEmpty(query)) {
      return Stream.concat(
          findMembers(memberDns),
          findPossibleMembers(memberDns, group.getPrimaryGroupId(), null)
      );
    }
    return findPossibleMembers(memberDns, group.getPrimaryGroupId(), query);
  }

  @Override
  public Stream<DomainGroupMember> getMembers(String groupName, Dn ou, TreeSearchScope searchScope) {
    DomainGroup group = findOne(groupName, ou, searchScope)
        .orElseThrow(() -> ServiceException.notFoundWithErrorCode(
            DomainGroup.class.getSimpleName(), groupName, EC_SAM_ACCOUNT_NOT_FOUND));
    return findMembers(new HashSet<>(group.getMembers()));
  }

  Stream<DomainGroupMember> findMembers(Set<String> memberDnSet) {
    log.debug("findMembers({})", memberDnSet);
    String[] returnAttributes = DomainGroupLdapMapper.DOMAIN_GROUP_MEMBER_ATTRIBUTES;
    return memberDnSet.stream()
        .flatMap(dn -> getLdapTemplate()
            .findOne(SearchRequest.objectScopeSearchRequest(dn, returnAttributes))
            .stream())
        .map(ldapEntry -> domainGroupLdapMapper.mapDomainGroupMember(ldapEntry, true));
  }

  Stream<DomainGroupMember> findPossibleMembers(Set<String> excludedDns, Integer groupId,
      String query) {
    log.debug("findPossibleMembers({})", excludedDns);
    String[] returnAttributes = DomainGroupLdapMapper.DOMAIN_GROUP_MEMBER_ATTRIBUTES;
    Filter findAllMembersFilter;
    Filter objectClassFilter = new OrFilter(
        new EqualityFilter(LDAP_OBJECT_CLASS, LDAP_OBJECT_CLASS_GROUP),
        new EqualityFilter(LDAP_OBJECT_CLASS, LDAP_OBJECT_CLASS_USER) // computers are also users
    );
    if (isEmpty(query) || query.length() <= 2) {
      findAllMembersFilter = objectClassFilter;
    } else {
      Filter queryFilter = new OrFilter(
          new SubstringFilter(LDAP_SAM_ACCOUNT_NAME, null, null, query),
          new SubstringFilter(LDAP_USER_GIVEN_NAME, null, null, query),
          new SubstringFilter(LDAP_USER_SN, null, null, query),
          new SubstringFilter(LDAP_USER_DISPLAY_NAME, null, null, query),
          new SubstringFilter(LDAP_NAME, null, null, query)
      );
      findAllMembersFilter = new AndFilter(objectClassFilter, queryFilter);
    }
    SearchRequest searchRequest = searchAllRequest(getProperties().getBaseDn(),
        findAllMembersFilter, SearchScope.SUBTREE, returnAttributes);
    return getLdapTemplate().findAll(searchRequest)
        .stream()
        .filter(getIgnoredEntryFilter())
        .filter(ldapEntry -> !excludedDns.contains(new Dn(ldapEntry.getDn()).format()))
        .map(ldapEntry -> domainGroupLdapMapper.mapDomainGroupMember(ldapEntry, false))
        .filter(member -> isEmpty(groupId)
            || !groupId.equals(member.getPrimaryGroupId()));
  }

  Optional<SamAccount> findSamAccount(String samAccountName, Dn ou, TreeSearchScope searchScope) {
    String[] returnAttributes = DomainGroupLdapMapper.SAM_ACCOUNT_ATTRIBUTES;
    SearchRequest searchRequest;
    if (getProperties().isDn(samAccountName)) {
      searchRequest = SearchRequest.objectScopeSearchRequest(samAccountName, returnAttributes);
    } else {
      Dn ouDn;
      SearchScope scope;
      if (isEmpty(ou) || ou.isEmpty()) {
        ouDn = getProperties().getBaseDn();
        scope = SearchScope.SUBTREE;
      } else {
        ouDn = getProperties().getBaseDn(ou);
        scope = requireNonNullElse(TreeSearchScopeConverter
            .toSearchScope(searchScope), SearchScope.SUBTREE);
      }
      Filter filter = new EqualityFilter(LDAP_SAM_ACCOUNT_NAME, samAccountName);
      searchRequest = searchOneRequest(samAccountName, ouDn, filter, scope, returnAttributes);
    }
    return getLdapTemplate().findOne(searchRequest)
        .filter(getIgnoredEntryFilter(ou, TreeSearchScopeConverter.toSearchScope(searchScope)))
        .map(domainGroupLdapMapper::mapSamAccount);
  }

  @Override
  public Optional<DomainGroup> findOne(String groupName, Dn ou, TreeSearchScope searchScope) {
    SearchScope scope = TreeSearchScopeConverter.toSearchScope(searchScope);
    SearchRequest searchRequest = searchOneRequest(groupName, ou, scope);
    return getLdapTemplate()
        .findOne(searchRequest, domainGroupLdapMapper)
        .filter(getIgnoredObjectFilter(ou, scope));
  }

  @Override
  public Optional<DomainGroup> findOneByPrimaryGroupId(Integer primaryGroupId) {
    log.debug("findByPrimaryGroupId({})", primaryGroupId);
    return Optional.ofNullable(primaryGroupId)
        .map(id -> domainRepository.getDomainSid() + "-" + primaryGroupId)
        .flatMap(sid -> {
          Filter filter = new AndFilter(
              objectClassFilter(),
              new EqualityFilter(LDAP_OBJECT_SID, sid));
          SearchRequest searchRequest = SearchRequest.builder()
              .dn(getProperties().getBaseDn().format())
              .filter(filter)
              .scope(SearchScope.SUBTREE)
              .binaryAttributes(getBinaryAttributes())
              .returnAttributes(getReturnAttributes())
              .build();
          return getLdapTemplate()
              .findOne(searchRequest, domainGroupLdapMapper)
              .filter(getIgnoredObjectFilter());
        });
  }

  public Optional<DomainGroup> findOneByGidNumber(Integer gidNumber) {
    log.debug("findByGidNumber({})", gidNumber);
    return Optional.ofNullable(gidNumber)
        .flatMap(gid -> {
          Filter filter = new AndFilter(
              objectClassFilter(),
              new EqualityFilter(LDAP_GID_NUMBER, gid.toString()));
          SearchRequest searchRequest = SearchRequest.builder()
              .dn(getProperties().getBaseDn().format())
              .filter(filter)
              .scope(SearchScope.SUBTREE)
              .binaryAttributes(getBinaryAttributes())
              .returnAttributes(getReturnAttributes())
              .build();
          return getLdapTemplate()
              .findOne(searchRequest, domainGroupLdapMapper)
              .filter(getIgnoredObjectFilter());
        });
  }

  public boolean existsByGidNumber(Integer gidNumber) {
    log.debug("existsByGidNumber({})", gidNumber);
    return Optional.ofNullable(gidNumber)
        .map(gid -> {
          Filter filter = new AndFilter(
              objectClassFilter(),
              new EqualityFilter(LDAP_GID_NUMBER, gid.toString()));
          SearchRequest searchRequest = SearchRequest.builder()
              .dn(getProperties().getBaseDn().format())
              .filter(filter)
              .scope(SearchScope.SUBTREE)
              .returnAttributes(new String[]{LDAP_GID_NUMBER})
              .build();
          return getLdapTemplate()
              .findOne(searchRequest)
              .filter(getIgnoredEntryFilter())
              .isPresent();
        })
        .orElse(false);
  }

  @ProfileRequired({"cli", "ldap"})
  @Override
  public DomainGroup add(DomainGroup domainGroup, Dn ou) {
    if (isEmpty(domainGroup.getSamAccountName())) {
      throw ServiceException.badRequest(
          "Group name (samAccountName) is required.", EC_SAM_ACCOUNT_NAME_REQUIRED);
    }
    if (domainGroup.getSamAccountName().contains(",")) {
      throw ServiceException.badRequest(
          "Group name (samAccountName) contains illegal characters.", EC_ILLEGAL_SAM_ACCOUNT_NAME);
    }
    if (getDomainRepository().samAccountNameExists(domainGroup.getSamAccountName())) {
      throw ServiceException.alreadyExistsWithErrorCode(
          DomainGroup.class.getSimpleName(),
          domainGroup.getSamAccountName(),
          EC_SAM_ACCOUNT_ALREADY_EXISTS);
    }
    if (existsByGidNumber(domainGroup.getGidNumber())) {
      throw ServiceException.alreadyExistsWithErrorCode(
          DomainGroup.class.getSimpleName() + ".gidNumber",
          domainGroup.getGidNumber(),
          EC_GID_NUMBER_ALREADY_EXISTS);
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
  String doAdd(DomainGroup domainGroup, Dn ouDn) {
    Dn ou = getProperties().removeBaseDn(validateOu(ouDn));
    kinit();
    final List<String> commands = new ArrayList<>();
    ssh(commands);
    sudo(commands);
    commands.add(getProperties().getSambaToolBinary());
    commands.add("group");
    commands.add("add");
    commands.add(quote(domainGroup.getSamAccountName()));
    Optional.ofNullable(domainGroup.getGroupType())
        .map(DomainGroupTypeContainer::getGroupType)
        .map(DomainGroupType::getScope)
        .ifPresent(scope -> commands.add("--group-scope=" + scope));
    Optional.ofNullable(domainGroup.getGroupType())
        .map(DomainGroupTypeContainer::getGroupType)
        .map(DomainGroupType::getPurpose)
        .ifPresent(purpose -> commands.add("--group-type=" + purpose));
    if (!isEmpty(ou) && !ou.isEmpty()) {
      commands.add("--groupou=" + quote(ou.format()));
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

  @Override
  public DomainGroup update(String groupName, DomainGroup domainGroup, Dn newOu) {
    log.debug("update({}, {}, {})", groupName, domainGroup.getSamAccountName(), newOu);
    if (isEmpty(domainGroup.getSamAccountName())) {
      throw ServiceException.badRequest(
          "Group name (samAccountName) is required.", EC_SAM_ACCOUNT_NAME_REQUIRED);
    }
    if (domainGroup.getSamAccountName().contains(",")) {
      throw ServiceException.badRequest(
          "Group name (samAccountName) contains illegal characters.", EC_ILLEGAL_SAM_ACCOUNT_NAME);
    }
    if (!groupName.equalsIgnoreCase(domainGroup.getSamAccountName())
        && getDomainRepository().samAccountNameExists(domainGroup.getSamAccountName())) {
      throw ServiceException.alreadyExistsWithErrorCode(
          DomainGroup.class.getSimpleName(),
          domainGroup.getSamAccountName(),
          EC_SAM_ACCOUNT_ALREADY_EXISTS);
    }
    DomainGroup existingDomainGroup = findOne(groupName, null, null)
        .orElseThrow(() -> ServiceException.notFoundWithErrorCode(
            DomainGroup.class.getSimpleName(),
            domainGroup.getSamAccountName(),
            EC_SAM_ACCOUNT_NOT_FOUND));
    if (!isEmpty(domainGroup.getGidNumber())
        && !Objects.equals(domainGroup.getGidNumber(), existingDomainGroup.getGidNumber())
        && existsByGidNumber(domainGroup.getGidNumber())) {
      throw ServiceException.alreadyExistsWithErrorCode(
          DomainGroup.class.getSimpleName() + ".gidNumber",
          domainGroup.getGidNumber(),
          EC_GID_NUMBER_ALREADY_EXISTS);
    }
    Dn oldDn = new Dn(existingDomainGroup.getDistinguishedName());
    Dn newDn = getNewDn(existingDomainGroup, domainGroup, newOu);
    if (!oldDn.isSame(newDn) && getDomainRepository().dnExistsWithAnyObjectClass(newDn.format())) {
      throw ServiceException.alreadyExistsWithErrorCode(
          DomainGroup.class.getSimpleName(),
          getProperties().removeBaseDn(newDn),
          EC_DN_ALREADY_EXISTS);
    }
    DomainGroup updatedDomainGroup = renameAndMove(existingDomainGroup, domainGroup, newDn);
    return getLdapTemplate().save(updatedDomainGroup, domainGroupLdapMapper);
  }

  Dn getNewDn(DomainGroup oldDomainGroup, DomainGroup newDomainGroup, Dn newOu) {
    Dn newParentDn;
    if (!isEmpty(newOu) && !newOu.isEmpty()) {
      newParentDn = getProperties().getBaseDn(validateOu(newOu));
    } else {
      newParentDn = getProperties().getParentDn(oldDomainGroup.getDistinguishedName());
    }

    RDn oldRdn = new Dn(oldDomainGroup.getDistinguishedName()).getRDn();
    String newCn = newDomainGroup.getSamAccountName();
    Dn newDn = new Dn(new RDn(new NameValue(oldRdn.getNameValue().getName(), newCn)));
    newDn.add(newParentDn);
    return newDn;
  }

  DomainGroup renameAndMove(DomainGroup oldDomainGroup, DomainGroup newDomainGroup, Dn newDn) {
    String oldCn = new Dn(oldDomainGroup.getDistinguishedName())
        .getRDn().getNameValue().getStringValue();
    String newCn = newDn
        .getRDn().getNameValue().getStringValue();
    boolean cnChanged = !Objects.equals(oldCn, newCn);
    Dn oldParentDn = getProperties().getParentDn(oldDomainGroup.getDistinguishedName());

    String oldSamAccountName = oldDomainGroup.getSamAccountName();
    String newSamAccountName = newDomainGroup.getSamAccountName();
    boolean samAccountNameChanged = !Objects.equals(oldSamAccountName, newSamAccountName);

    String oldEmail = oldDomainGroup.getEmail();
    String newEmail = newDomainGroup.getEmail();
    boolean emailChanged = !Objects.equals(oldEmail, newEmail);

    if (cnChanged || samAccountNameChanged || emailChanged) {
      kinit();
      List<String> commands = new ArrayList<>();
      ssh(commands);
      sudo(commands);
      commands.add(getProperties().getSambaToolBinary());
      commands.add("group");
      commands.add("rename");
      commands.add(quote(oldSamAccountName));
      if (samAccountNameChanged) {
        commands.add("--samaccountname=" + quote(newSamAccountName));
      }
      if (cnChanged) {
        commands.add("--force-new-cn=" + quote(newCn));
      }
      if (emailChanged) {
        commands.add(" --mail-address=" + quote(oldEmail));
      }
      auth(commands);
      CommandExecutor.exec(
          commands,
          null,
          getProperties().getSambaToolExecDir(),
          (CommandExecutorResponseValidator) response -> this
              .findOne(
                  newSamAccountName,
                  oldParentDn,
                  TreeSearchScope.ONELEVEL)
              .orElseThrow(() -> ServiceException
                  .internalServerError(String.format("Updating names of group '%s' failed. %s",
                          newDomainGroup.getSamAccountName(),
                          CommandExecutorResponse.toExceptionMessage(response)),
                      EC_UPDATING_GROUP_FAILED)));
    }
    Dn newParentDn = getProperties().getParentDn(newDn.format());
    if (!oldParentDn.isSame(newParentDn)) {
      String ou = getProperties().removeBaseDn(newParentDn).format();
      kinit();
      List<String> commands = new ArrayList<>();
      ssh(commands);
      sudo(commands);
      commands.add(getProperties().getSambaToolBinary());
      commands.add("group");
      commands.add("move");
      commands.add(quote(newSamAccountName));
      commands.add(quote(ou));
      auth(commands);

      CommandExecutor.exec(
          commands,
          null,
          getProperties().getSambaToolExecDir(),
          (CommandExecutorResponseValidator) response -> getDomainRepository()
              .findDnOfSamAccountName(newSamAccountName)
              .filter(groupDn -> new Dn(groupDn).isSame(newDn))
              .orElseThrow(() -> ServiceException
                  .internalServerError(String.format("Moving group '%s' to '%s' failed. %s",
                          newSamAccountName, ou,
                          CommandExecutorResponse.toExceptionMessage(response)),
                      EC_UPDATING_GROUP_FAILED)));
    }
    newDomainGroup.setDistinguishedName(newDn.format());
    return newDomainGroup;
  }

  // TODO
  public boolean hasAllNisAttributes(DomainGroup domainGroup) {
    return !isEmpty(domainGroup)
        && !isEmpty(getNisDomain(domainGroup))
        && !isEmpty(domainGroup.getGidNumber());
  }

  @ProfileRequired({"cli", "ldap"})
  @Override
  public boolean delete(String groupName) {
    log.debug("delete({})", groupName);
    return findOne(groupName, null, null)
        .filter(group -> !isEmpty(group.getSid()))
        .filter(group -> Optional.of(group.getSid())
            .map(sid -> !sid.getSystemEntity())
            .orElseThrow(() -> ServiceException.badRequest(
                String.format(
                    "'%s' is a system group. Deletion failed.",
                    group.getSamAccountName()),
                EC_ILLEGAL_SYSTEM_ENTITY_OPERATION)))
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
