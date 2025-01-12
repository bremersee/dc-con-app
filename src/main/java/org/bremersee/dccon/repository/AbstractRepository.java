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

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElseGet;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.Getter;
import org.bremersee.dccon.ErrorCode;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.CommonAttributes;
import org.bremersee.dccon.model.DistinguishedNameProvider;
import org.bremersee.dccon.model.NisDomainMember;
import org.bremersee.dccon.repository.cli.CommandExecutor;
import org.bremersee.exception.ServiceException;
import org.bremersee.ldaptive.LdaptiveException;
import org.bremersee.ldaptive.LdaptiveTemplate;
import org.ldaptive.LdapEntry;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
import org.ldaptive.filter.AndFilter;
import org.ldaptive.filter.EqualityFilter;
import org.ldaptive.filter.Filter;
import org.springframework.util.Assert;

/**
 * The abstract repository.
 *
 * @author Christian Bremer
 */
abstract class AbstractRepository implements ErrorCode, RepositoryConstants {

  private static final Object KINIT_LOCK = new Object();

  private static final String KINIT_PASSWORD_FILE = "--password-file={}";

  private static final String USE_KERBEROS = "-k";

  private static final String YES = "yes";

  @Getter(AccessLevel.PACKAGE)
  private final DomainControllerProperties properties;

  @Getter(AccessLevel.PACKAGE)
  private final LdaptiveTemplate ldapTemplate;

  @Getter(AccessLevel.PACKAGE)
  private final Predicate<String> ignoredDnFilter;

  @Getter(AccessLevel.PACKAGE)
  private final Predicate<LdapEntry> ignoredEntryFilter;

  @Getter(AccessLevel.PACKAGE)
  private final Predicate<DistinguishedNameProvider> ignoredObjectFilter;

  /**
   * Instantiates a new abstract repository.
   *
   * @param properties the properties
   * @param ldapTemplate the ldap template
   */
  AbstractRepository(
      DomainControllerProperties properties,
      LdaptiveTemplate ldapTemplate) {
    Assert.notNull(properties, "Domain controller properties must not be present.");
    this.properties = properties;
    this.ldapTemplate = ldapTemplate;
    this.ignoredDnFilter = dn -> isEmpty(dn) || Arrays
        .stream(DomainControllerProperties.IGNORED_DN)
        .map(this.properties::getBaseDn)
        .noneMatch(ignoredDn -> ignoredDn.isAncestor(new Dn(dn)));
    this.ignoredEntryFilter = entry -> ignoredDnFilter.test(entry.getDn());
    this.ignoredObjectFilter = distinguishedNameProvider -> ignoredDnFilter
        .test(distinguishedNameProvider.getDistinguishedName());
  }

  private boolean isIgnoredDnFilterRequired(Dn ou, SearchScope scope) {
    if (nonNull(scope) && scope != SearchScope.SUBTREE) {
      return false;
    }
    return isEmpty(ou) || ou.isEmpty() || getProperties().getBaseDn()
        .isSame(getProperties().getBaseDn(ou));
  }

  Predicate<String> getIgnoredDnFilter(Dn ou, SearchScope scope) {
    if (isIgnoredDnFilterRequired(ou, scope)) {
      return getIgnoredDnFilter();
    }
    return dn -> true;
  }

  Predicate<LdapEntry> getIgnoredEntryFilter(Dn ou, SearchScope scope) {
    if (isIgnoredDnFilterRequired(ou, scope)) {
      return getIgnoredEntryFilter();
    }
    return entry -> true;
  }

  Predicate<DistinguishedNameProvider> getIgnoredObjectFilter(Dn ou, SearchScope scope) {
    if (isIgnoredDnFilterRequired(ou, scope)) {
      return getIgnoredObjectFilter();
    }
    return distinguishedNameProvider -> true;
  }

  /**
   * Calls linux command {@code kinit} for authentication.
   */
  void kinit() {
    if (getProperties().isUsingKinit()) {
      synchronized (KINIT_LOCK) {
        List<String> commands = new ArrayList<>();
        sudo(commands);
        commands.add(properties.getKinitBinary());
        commands.add(KINIT_PASSWORD_FILE.replace("{}", properties.getKinitPasswordFile()));
        commands.add(properties.getKinitAdministratorName());
        CommandExecutor.exec(commands, properties.getSambaToolExecDir());
      }
    }
  }

  void ssh(List<String> commands) {
    if (getProperties().isUsingSsh()) {
      Assert.isTrue(!properties.isUsingKinit(), "Using ssh with kinit is not supported.");
      commands.add(properties.getSshCommand());
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
    if (getProperties().isUsingKinit()) {
      commands.add(USE_KERBEROS);
      commands.add(YES);
    }
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

  String validateDn(CommonAttributes object, String dn) {
    if (isEmpty(object.getDistinguishedName())) {
      object.setDistinguishedName(dn);
      return dn;
    }
    try {
      if (new Dn(dn).isSame(new Dn(object.getDistinguishedName()))) {
        return dn;
      }

    } catch (RuntimeException e) {
      // ignored
    }
    throw ServiceException.badRequest(String.format("Distinguished name of object '%s' is not "
            + "the same distinguished name of the ldap entry '%s'.",
        object.getDistinguishedName(), dn), EC_ILLEGAL_DN);
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


  static String quote(String value) {
    if (isEmpty(value)) {
      return "\"\"";
    }
    if (value.contains("\"")) {
      return '\'' + value + '\'';
    }
    return '"' + value + '"';
  }

  /**
   * Checks whether the given value contains the given query.
   *
   * @param value the value
   * @param query the query
   * @return {@code true} if the value contains the query, otherwise {@code false}
   */
  static boolean contains(Object value, String query) {
    if (isEmpty(value) || isEmpty(query)) {
      return false;
    }
    if (value instanceof Collection) {
      //noinspection rawtypes
      for (Object item : (Collection) value) {
        if (contains(item, query)) {
          return true;
        }
      }
      return false;
    }
    return value.toString().toLowerCase().contains(query.toLowerCase());
  }

}
