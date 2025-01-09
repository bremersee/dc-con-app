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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.PasswordInformation;
import org.bremersee.dccon.model.Sid;
import org.bremersee.dccon.repository.automock.MockComponent;
import org.bremersee.dccon.repository.automock.ProfileRequired;
import org.bremersee.dccon.repository.cli.CommandExecutor;
import org.bremersee.dccon.repository.cli.PasswordInformationParser;
import org.bremersee.dccon.repository.transcoder.SidValueTranscoder;
import org.bremersee.ldaptive.LdaptiveEntryMapper;
import org.bremersee.ldaptive.LdaptiveTemplate;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchScope;
import org.ldaptive.ad.SecurityIdentifier;
import org.ldaptive.dn.Dn;
import org.ldaptive.filter.EqualityFilter;
import org.ldaptive.filter.PresenceFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * The domain repository implementation.
 *
 * @author Christian Bremer
 */
@Primary
@Component("domainRepository")
@ProfileRequired("ldap")
@MockComponent(value = DomainRepositoryMock.class, methodsOf = DomainRepository.class)
@Slf4j
public class DomainRepositoryImpl extends AbstractDomainRepository
    implements DomainRepository {

  private PasswordInformationParser passwordInformationParser;

  /**
   * Instantiates a domain repository.
   *
   * @param properties the properties
   */
  public DomainRepositoryImpl(
      DomainControllerProperties properties,
      ObjectProvider<LdaptiveTemplate> ldapTemplateProvider) {
    super(properties, ldapTemplateProvider.getIfAvailable());
    this.passwordInformationParser = PasswordInformationParser.defaultParser();
  }

  /**
   * Sets password information parser.
   *
   * @param passwordInformationParser the password information parser
   */
  @Autowired(required = false)
  public void setPasswordInformationParser(
      PasswordInformationParser passwordInformationParser) {
    if (passwordInformationParser != null) {
      this.passwordInformationParser = passwordInformationParser;
    }
  }

  @Override
  public boolean dnExistsWithAnyObjectClass(String dn, String... objectClasses) {
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

  @Override
  public Optional<String> findDnOfSamAccountName(String samAccountName) {
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

  @Override
  public String getDomainSid() {
    String baseDn = getProperties().getBaseDn().format();
    String[] returnAttributes = new String[] {
        LDAP_OBJECT_SID
    };
    return getLdapTemplate()
        .findOne(SearchRequest.objectScopeSearchRequest(baseDn, returnAttributes))
        .map(ldapEntry -> ldapEntry.getAttribute(LDAP_OBJECT_SID))
        .map(LdapAttribute::getBinaryValue)
        .map(SecurityIdentifier::toString)
        .orElseThrow(() -> new IllegalStateException("Could not find domain sid")); // TODO
  }

  @Override
  public boolean isRfc2307Enabled() {
    Dn dn = new Dn("CN=ypservers,CN=ypServ30,CN=RpcServices,CN=System");
    dn.add(getProperties().getBaseDn());
    boolean result = dnExistsWithAnyObjectClass(dn.format());
    log.debug("Are nis extensions (rfc2307) installed? {}", result);
    return result;
  }

  @ProfileRequired("cli")
  @Override
  public PasswordInformation getPasswordInformation() {
    log.debug("getPasswordInformation()");
    kinit();
    List<String> commands = new ArrayList<>();
    ssh(commands);
    sudo(commands);
    commands.add(getProperties().getSambaToolBinary());
    commands.add("domain");
    commands.add("passwordsettings");
    commands.add("show");
    auth(commands);
    PasswordInformation raw = CommandExecutor.exec(
        commands,
        null,
        getProperties().getSambaToolExecDir(),
        passwordInformationParser);
    int minLength = raw.getMinimumPasswordLength();
    int maxLength = Math.max(getProperties().getMaximumPasswordLength(), minLength);
    return raw.toBuilder()
        .maximumPasswordLength(maxLength)
        .simplePasswordRegexTemplate(getProperties().getSimplePasswordRegexTemplate())
        .complexPasswordRegexTemplate(getProperties().getComplexPasswordRegexTemplate())
        .build();
  }

}
