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
import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.ErrorCode;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.CommonAttributes;
import org.bremersee.dccon.model.DistinguishedNameProvider;
import org.bremersee.dccon.repository.cli.CommandExecutor;
import org.bremersee.dccon.repository.cli.CommandExecutorResponse;
import org.bremersee.dccon.repository.cli.CommandExecutorResponseParser;
import org.bremersee.dccon.repository.cli.CommandExecutorResponseValidator;
import org.bremersee.exception.ServiceException;
import org.bremersee.ldaptive.LdaptiveTemplate;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
import org.ldaptive.filter.PresenceFilter;
import org.springframework.util.Assert;

/**
 * The abstract repository.
 *
 * @author Christian Bremer
 */
@Slf4j
abstract class AbstractRepository implements ErrorCode, RepositoryConstants {

  private static final Object KINIT_LOCK = new Object();

  private static final String KINIT_PASSWORD_FILE = "--password-file={}";

  private static final String USE_KERBEROS = "--use-kerberos=required";

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
  private void kinit() {
    synchronized (KINIT_LOCK) {
      List<String> commands = new ArrayList<>();
      if (getProperties().getCli().getSudo().isUsingSudo()) {
        commands.add(properties.getCli().getSudo().getSudoCommand());
      }
      commands.add(properties.getCli().getKinit().getKinitBinary());
      commands.add(KINIT_PASSWORD_FILE.replace("{}",
          properties.getCli().getKinit().getKinitPasswordFile()));
      commands.add(properties.getCli().getKinit().getKinitAdministratorName());
      CommandExecutor.exec(commands, properties.getCli().getExecDir());
    }
  }

  CommandExecutorResponse execute(List<String> commands) {
    return executeAndGet(commands, response -> response);
  }

  void execute(List<String> commands, CommandExecutorResponseValidator responseValidator) {
    executeAndGet(commands, (CommandExecutorResponseParser<?>) responseValidator);
  }

  <T> T executeAndGet(List<String> commands, CommandExecutorResponseParser<T> responseParser) {
    if (isEmpty(commands)) {
      return null;
    }
    boolean isSambaToolCommand = commands.stream()
        .anyMatch(cmd -> cmd
            .equalsIgnoreCase(getProperties().getCli().getSambaToolBinary()));
    List<String> extendedCommands = new ArrayList<>();
    if (isSambaToolCommand && getProperties().getCli().getKinit().isUsingKinit()) {
      kinit();
    } else if (getProperties().getCli().getSsh().isUsingSsh()) {
      extendedCommands.add(properties.getCli().getSsh().getSshCommand());
    }
    if (getProperties().getCli().getSudo().isUsingSudo()) {
      extendedCommands.add(properties.getCli().getSudo().getSudoCommand());
    }
    extendedCommands.addAll(commands);
    if (isSambaToolCommand && getProperties().getCli().getKinit().isUsingKinit()) {
      extendedCommands.add(USE_KERBEROS);
    } else if (isSambaToolCommand) {
      boolean needsSambaToolCredentials = commands.stream()
          .anyMatch(cmd -> cmd.equalsIgnoreCase("dns"));
      if (needsSambaToolCredentials) {
        extendedCommands.add(getProperties().getCli().getSambaToolCredentialsOptions());
      }
    }
    return CommandExecutor.exec(
        extendedCommands,
        null,
        getProperties().getCli().getExecDir(),
        responseParser);
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


  boolean dnExistsWithAnyObjectClass(String dn, String... objectClasses) {
    log.debug("dnExistsWithAnyObjectClass({}, {})", dn, objectClasses);
    if (!getProperties().isDn(dn)) {
      log.debug("Dn '{}' does not exist", dn);
      return false;
    }
    SearchRequest searchRequest = SearchRequest.builder()
        .dn(dn)
        .filter(new PresenceFilter(RepositoryConstants.LDAP_OBJECT_CLASS))
        .scope(SearchScope.OBJECT)
        .returnAttributes(RepositoryConstants.LDAP_OBJECT_CLASS)
        .sizeLimit(1)
        .build();
    log.debug("dnExistsWithAnyObjectClass, searchRequest = {}", searchRequest);
    return getLdapTemplate().findOne(searchRequest)
        .filter(getIgnoredEntryFilter())
        .map(ldapEntry -> {
          Set<String> wantedObjectClasses = Stream.ofNullable(objectClasses)
              .flatMap(Arrays::stream)
              .filter(cls -> !isEmpty(cls))
              .map(String::toLowerCase)
              .collect(Collectors.toSet());
          if (wantedObjectClasses.isEmpty()) {
            return true;
          }
          return Stream
              .ofNullable(ldapEntry.getAttribute(RepositoryConstants.LDAP_OBJECT_CLASS))
              .map(LdapAttribute::getStringValues)
              .flatMap(Collection::stream)
              .filter(cls -> !isEmpty(cls))
              .map(String::toLowerCase)
              .anyMatch(wantedObjectClasses::contains);
        })
        .orElse(false);
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
