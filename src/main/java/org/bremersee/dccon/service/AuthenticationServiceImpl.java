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

package org.bremersee.dccon.service;

import java.util.function.Function;
import org.bremersee.data.ldaptive.LdaptiveConnectionConfigFactory;
import org.bremersee.data.ldaptive.LdaptiveEntryMapper;
import org.bremersee.data.ldaptive.LdaptiveProperties;
import org.bremersee.data.ldaptive.LdaptiveTemplate;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.ldaptive.ConnectionConfig;
import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.SearchRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * The authentication service.
 *
 * @author Christian Bremer
 */
@Profile("ldap")
@Component("authenticationService")
public class AuthenticationServiceImpl implements AuthenticationService {

  private LdaptiveProperties ldaptiveProperties;

  private DomainControllerProperties domainControllerProperties;

  private Function<String, String> baseDnProvider;

  /**
   * Instantiates a new authentication service.
   *
   * @param ldaptivePropertiesProvider the ldaptive properties provider
   * @param domainControllerProperties the domain controller properties
   */
  public AuthenticationServiceImpl(
      final ObjectProvider<LdaptiveProperties> ldaptivePropertiesProvider,
      final DomainControllerProperties domainControllerProperties) {
    this.ldaptiveProperties = ldaptivePropertiesProvider.getIfAvailable();
    Assert.notNull(this.ldaptiveProperties, "Ldaptive properties must be present.");
    this.domainControllerProperties = domainControllerProperties;
    this.baseDnProvider = userName -> userName + "@" + domainControllerProperties.getDefaultZone();
  }

  /**
   * Sets base dn provider.
   *
   * @param baseDnProvider the base dn provider
   */
  @SuppressWarnings("unused")
  public void setBaseDnProvider(final Function<String, String> baseDnProvider) {
    if (baseDnProvider != null) {
      this.baseDnProvider = baseDnProvider;
    }
  }

  @Override
  public boolean passwordMatches(final String userName, final String clearPassword) {
    if (!StringUtils.hasText(userName) || !StringUtils.hasText(clearPassword)) {
      return false;
    }
    final String baseDn = baseDnProvider.apply(userName);
    try {
      final ConnectionConfig connectionConfig = LdaptiveConnectionConfigFactory.defaultFactory()
          .createConnectionConfig(ldaptiveProperties, baseDn, clearPassword);
      final DefaultConnectionFactory connectionFactory = new DefaultConnectionFactory();
      connectionFactory.setConnectionConfig(connectionConfig);
      final String dn = LdaptiveEntryMapper.createDn(
          domainControllerProperties.getUserRdn(),
          userName,
          domainControllerProperties.getUserBaseDn());
      final SearchRequest searchRequest = SearchRequest.newObjectScopeSearchRequest(dn);
      final LdaptiveTemplate ldaptiveTemplate = new LdaptiveTemplate(connectionFactory);
      return ldaptiveTemplate.findOne(searchRequest).isPresent();

    } catch (final Exception e) {
      return false;
    }
  }

}
