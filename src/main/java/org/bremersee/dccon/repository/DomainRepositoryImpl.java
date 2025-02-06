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
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.PasswordInformation;
import org.bremersee.dccon.repository.automock.MockComponent;
import org.bremersee.dccon.repository.automock.ProfileRequired;
import org.bremersee.dccon.repository.cli.parser.PasswordInformationParser;
import org.bremersee.dccon.repository.cli.parser.HostNameResponseParser;
import org.bremersee.ldaptive.LdaptiveTemplate;
import org.ldaptive.LdapAttribute;
import org.ldaptive.SearchRequest;
import org.ldaptive.ad.SecurityIdentifier;
import org.ldaptive.dn.Dn;
import org.springframework.beans.factory.ObjectProvider;
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

  private final HostNameResponseParser hostNameResponseParser;

  private final PasswordInformationParser passwordInformationParser;

  private String hostName;

  /**
   * Instantiates a domain repository.
   *
   * @param properties the properties
   */
  public DomainRepositoryImpl(
      DomainControllerProperties properties,
      ObjectProvider<LdaptiveTemplate> ldapTemplateProvider) {
    super(properties, ldapTemplateProvider.getIfAvailable());
    this.hostNameResponseParser = HostNameResponseParser.defaultParser();
    this.passwordInformationParser = PasswordInformationParser.defaultParser();
  }

  @ProfileRequired("cli")
  @Override
  public String getHostName() {
    if (isEmpty(hostName)) {
      hostName = Optional.ofNullable(getProperties().getHostName())
          .filter(name -> !name.isBlank())
          .orElseGet(() -> {
            List<String> commands = new ArrayList<>(2);
            commands.add(getProperties().getCli().getHostnameBinary());
            if (!isEmpty(getProperties().getCli().getHostnameOptions())) {
              commands.add(getProperties().getCli().getHostnameOptions());
            }
            return executeAndGet(commands, hostNameResponseParser);
          });
    }
    return hostName;
  }

  @Override
  public String getDomainSid() {
    String baseDn = getProperties().getBaseDn().format();
    String[] returnAttributes = new String[]{
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
    List<String> commands = new ArrayList<>();
    commands.add(getProperties().getCli().getSambaToolBinary());
    commands.add("domain");
    commands.add("passwordsettings");
    commands.add("show");
    PasswordInformation raw = executeAndGet(
        commands,
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
