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

import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainGroupMember;
import org.bremersee.dccon.model.DomainGroupMemberType;
import org.bremersee.dccon.model.SelectOption;
import org.bremersee.ldaptive.LdaptiveEntryMapper;
import org.bremersee.ldaptive.LdaptiveTemplate;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchScope;
import org.ldaptive.filter.EqualityFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * The domain group repository.
 *
 * @author Christian Bremer
 */
@Profile("ldap")
@Component("domainGroupMemberRepository")
@Slf4j
public class DomainGroupMemberRepositoryImpl extends AbstractDomainGroupRepository
    implements DomainGroupMemberRepository {

  /**
   * Instantiates a new domain group repository.
   *
   * @param properties the properties
   * @param ldapTemplateProvider the ldap template provider
   */
  public DomainGroupMemberRepositoryImpl(
      final DomainControllerProperties properties,
      final ObjectProvider<LdaptiveTemplate> ldapTemplateProvider,
      DomainRepository domainRepository) {
    super(properties, ldapTemplateProvider.getIfAvailable(), domainRepository);
  }

  @Override
  public Stream<SelectOption<DomainGroupMember>> getMemberSelectOptions(
      String groupName,
      DomainGroupMemberType... types) {

    Set<String> dnOfMembers = getDistinguishedNameOfMembers(groupName)
        .collect(Collectors.toSet());

    Stream<SelectOption<DomainGroupMember>> result = getMemberSelectOptionsOfSelected(
        dnOfMembers, types);
    result = Stream.concat(result, getMemberSelectOptionsOfUsers(dnOfMembers, types));
    result = Stream.concat(result, getMemberSelectOptionsOfGroups(dnOfMembers, types));
    return Stream.concat(result, getMemberSelectOptionsOfComputers(dnOfMembers, types));
  }

  private Stream<String> getDistinguishedNameOfMembers(String groupName) {
    SearchRequest searchRequest = searchOneRequest(groupName, LDAP_GROUP_MEMBER);
    return getLdapTemplate().findOne(searchRequest)
        .filter(getNoBuiltinEntryFilter())
        .map(ldapEntry -> ldapEntry.getAttribute(LDAP_GROUP_MEMBER))
        .map(LdapAttribute::getStringValues)
        .stream()
        .flatMap(Collection::stream);
  }

  private Stream<SelectOption<DomainGroupMember>> getMemberSelectOptionsOfSelected(
      Set<String> dnOfMembers, DomainGroupMemberType... types) {

    Predicate<LdapEntry> process = ldapEntry -> dnOfMembers.contains(ldapEntry.getDn());
    Predicate<String> selected = dn -> true;
    return dnOfMembers
        .parallelStream()
        .map(dn -> SearchRequest.builder()
            .dn(dn)
            .scope(SearchScope.OBJECT)
            .returnAttributes(
                LDAP_OBJECT_CLASS,
                LDAP_SAM_ACCOUNT_NAME,
                DomainUserRepositoryConstants.LDAP_USER_GIVEN_NAME,
                DomainUserRepositoryConstants.LDAP_USER_SN,
                DomainUserRepositoryConstants.LDAP_USER_DISPLAY_NAME,
                LDAP_NAME)
            .build())
        .flatMap(searchRequest -> getMemberSelectOptions(searchRequest, process, selected, types));
  }

  private Stream<SelectOption<DomainGroupMember>> getMemberSelectOptionsOfUsers(
      Set<String> dnOfMembers, DomainGroupMemberType... types) {

    SearchRequest searchRequest = SearchRequest.builder()
        .dn(getProperties().getBaseDn().format())
        // TODO computers are also users!
        .filter(new EqualityFilter(LDAP_OBJECT_CLASS, RepositoryConstants.LDAP_OBJECT_CLASS_USER))
        .scope(SearchScope.SUBTREE)
        .returnAttributes(
            LDAP_OBJECT_CLASS,
            LDAP_SAM_ACCOUNT_NAME,
            DomainUserRepositoryConstants.LDAP_USER_GIVEN_NAME,
            DomainUserRepositoryConstants.LDAP_USER_SN,
            DomainUserRepositoryConstants.LDAP_USER_DISPLAY_NAME,
            LDAP_NAME)
        .build();
    Predicate<LdapEntry> process = ldapEntry -> !dnOfMembers.contains(ldapEntry.getDn());
    Predicate<String> selected = dn -> false;
    return getMemberSelectOptions(searchRequest, process, selected, types);
  }

  private Stream<SelectOption<DomainGroupMember>> getMemberSelectOptionsOfGroups(
      Set<String> dnOfMembers, DomainGroupMemberType... types) {

    SearchRequest searchRequest = SearchRequest.builder()
        .dn(getProperties().getBaseDn().format())
        .filter(new EqualityFilter(LDAP_OBJECT_CLASS, LDAP_OBJECT_CLASS_GROUP))
        .scope(SearchScope.SUBTREE)
        .returnAttributes(
            LDAP_OBJECT_CLASS,
            LDAP_SAM_ACCOUNT_NAME,
            LDAP_NAME)
        .build();
    Predicate<LdapEntry> process = ldapEntry -> !dnOfMembers.contains(ldapEntry.getDn());
    Predicate<String> selected = dn -> false;
    return getMemberSelectOptions(searchRequest, process, selected, types);
  }

  private Stream<SelectOption<DomainGroupMember>> getMemberSelectOptionsOfComputers(
      Set<String> dnOfMembers, DomainGroupMemberType... types) {

    SearchRequest searchRequest = SearchRequest.builder()
        .dn(getProperties().getBaseDn().format())
        .filter(
            new EqualityFilter(LDAP_OBJECT_CLASS, RepositoryConstants.LDAP_OBJECT_CLASS_COMPUTER))
        .scope(SearchScope.SUBTREE)
        .returnAttributes(
            LDAP_OBJECT_CLASS,
            LDAP_SAM_ACCOUNT_NAME,
            LDAP_NAME)
        .build();
    Predicate<LdapEntry> process = ldapEntry -> !dnOfMembers.contains(ldapEntry.getDn());
    Predicate<String> selected = dn -> false;
    return getMemberSelectOptions(searchRequest, process, selected, types);
  }

  private Stream<SelectOption<DomainGroupMember>> getMemberSelectOptions(
      SearchRequest searchRequest,
      Predicate<LdapEntry> process,
      Predicate<String> selected,
      DomainGroupMemberType... types) {

    Set<DomainGroupMemberType> supportedTypes = getSupportedTypes(types);
    return getLdapTemplate().findAll(searchRequest)
        .parallelStream()
        .filter(getNoBuiltinEntryFilter())
        .filter(process)
        .map(entry -> {
          DomainGroupMember member = DomainGroupMember.builder()
              .distinguishedName(entry.getDn())
              .objectClass(Optional
                  .ofNullable(entry.getAttribute(LDAP_OBJECT_CLASS))
                  .map(LdapAttribute::getStringValues)
                  .map(DomainGroupMemberType::fromObjectClasses)
                  .orElse(DomainGroupMemberType.UNKNOWN))
              .name(getMemberName(entry))
              .displayName(getMemberDisplayName(entry))
              .build();
          return new SelectOption<>(
              member.getDistinguishedName(),
              member,
              getMemberSortValue(entry),
              selected.test(member.getDistinguishedName()),
              false,
              !supportedTypes.contains(member.getObjectClass()));
        });
  }

  private Set<DomainGroupMemberType> getSupportedTypes(DomainGroupMemberType... types) {
    if (isEmpty(types)) {
      return Arrays.stream(DomainGroupMemberType.values())
          .filter(type -> type != DomainGroupMemberType.UNKNOWN)
          .collect(Collectors.toSet());
    }
    return Arrays.stream(types).collect(Collectors.toSet());
  }

  private String getMemberName(LdapEntry member) {
    return Optional.ofNullable(member.getAttribute(LDAP_SAM_ACCOUNT_NAME))
        .map(LdapAttribute::getStringValue)
        .orElseGet(() -> LdaptiveEntryMapper.getRdn(member.getDn()));
  }

  private String getMemberDisplayName(LdapEntry member) {
    return Optional.ofNullable(
            member.getAttribute(DomainUserRepositoryConstants.LDAP_USER_GIVEN_NAME))
        .map(LdapAttribute::getStringValue)
        .flatMap(firstName -> Optional
            .ofNullable(member.getAttribute(DomainUserRepositoryConstants.LDAP_USER_SN))
            .map(LdapAttribute::getStringValue)
            .map(lastName -> firstName + " " + lastName))
        .or(() -> Optional.ofNullable(
                member.getAttribute(DomainUserRepositoryConstants.LDAP_USER_DISPLAY_NAME))
            .map(LdapAttribute::getStringValue))
        .or(() -> Optional.ofNullable(member.getAttribute(LDAP_NAME))
            .map(LdapAttribute::getStringValue))
        .orElseGet(() -> LdaptiveEntryMapper.getRdn(member.getDn()));
  }

  private String getMemberSortValue(LdapEntry member) {
    return Optional.ofNullable(
            member.getAttribute(DomainUserRepositoryConstants.LDAP_USER_GIVEN_NAME))
        .map(LdapAttribute::getStringValue)
        .flatMap(firstName -> Optional
            .ofNullable(member.getAttribute(DomainUserRepositoryConstants.LDAP_USER_SN))
            .map(LdapAttribute::getStringValue)
            .map(lastName -> lastName + ", " + firstName))
        .orElse(null); // null = use member name
  }

  @Override
  public Optional<DomainGroupMember> findMember(String distinguishedNameOfMember) {
    return getMemberSelectOptionsOfSelected(Set.of(distinguishedNameOfMember))
        .map(SelectOption::getDisplayValue)
        .findFirst();
  }

  @Override
  public DomainGroupMember getMember(String distinguishedNameOfMember) {
    return findMember(distinguishedNameOfMember)
        .orElseGet(() -> DomainGroupMember.builder()
            .distinguishedName(distinguishedNameOfMember)
            .objectClass(DomainGroupMemberType.UNKNOWN)
            .name(LdaptiveEntryMapper.getRdn(distinguishedNameOfMember))
            .build());
  }

}
