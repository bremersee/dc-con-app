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

import static org.bremersee.dccon.repository.cli.CommandExecutorResponse.toExceptionMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.data.ldaptive.LdaptiveEntryMapper;
import org.bremersee.data.ldaptive.LdaptiveTemplate;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.repository.cli.CommandExecutor;
import org.bremersee.dccon.repository.cli.CommandExecutorResponse;
import org.bremersee.dccon.repository.cli.CommandExecutorResponseParser;
import org.bremersee.dccon.repository.cli.CommandExecutorResponseValidator;
import org.bremersee.dccon.repository.ldap.DnsZoneLdapMapper;
import org.bremersee.exception.ServiceException;
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * The dns zone repository.
 *
 * @author Christian Bremer
 */
@Profile("ldap")
@Component("dnsZoneRepository")
@Slf4j
public class DnsZoneRepositoryImpl extends AbstractRepository implements DnsZoneRepository {

  private final List<Pattern> excludedZoneNamePatterns;

  private LdaptiveEntryMapper<DnsZone> dnsZoneLdapMapper;

  /**
   * Instantiates a new dns zone repository.
   *
   * @param properties   the properties
   * @param ldapTemplate the ldap template
   */
  public DnsZoneRepositoryImpl(
      final DomainControllerProperties properties,
      final LdaptiveTemplate ldapTemplate) {
    super(properties, ldapTemplate);
    this.dnsZoneLdapMapper = new DnsZoneLdapMapper(properties);
    this.excludedZoneNamePatterns = properties.getExcludedZoneRegexList().stream()
        .map(Pattern::compile).collect(Collectors.toList());
  }

  /**
   * Sets dns zone ldap mapper.
   *
   * @param dnsZoneLdapMapper the dns zone ldap mapper
   */
  @SuppressWarnings("unused")
  public void setDnsZoneLdapMapper(final LdaptiveEntryMapper<DnsZone> dnsZoneLdapMapper) {
    if (dnsZoneLdapMapper != null) {
      this.dnsZoneLdapMapper = dnsZoneLdapMapper;
    }
  }

  private boolean isNonExcludedDnsZone(final DnsZone zone) {
    return zone != null && !isExcludedDnsZone(zone);
  }

  private boolean isNonExcludedDnsZone(final String zoneName) {
    return zoneName != null && !isExcludedDnsZone(zoneName);
  }

  private boolean isExcludedDnsZone(final DnsZone zone) {
    return zone != null && isExcludedDnsZone(zone.getName());
  }

  private boolean isExcludedDnsZone(final String zoneName) {
    return zoneName != null && excludedZoneNamePatterns.stream()
        .anyMatch(pattern -> pattern.matcher(zoneName).matches());
  }

  @Override
  public boolean isDnsReverseZone(final String dnsZoneName) {
    return dnsZoneName != null && getProperties().getReverseZoneSuffixList().stream()
        .anyMatch(suffix -> dnsZoneName.toLowerCase().endsWith(suffix.toLowerCase()));
  }

  @Override
  public Stream<DnsZone> findAll() {
    final SearchRequest searchRequest = new SearchRequest(
        getProperties().getDnsZoneBaseDn(),
        new SearchFilter(getProperties().getDnsZoneFindAllFilter()));
    searchRequest.setSearchScope(getProperties().getDnsZoneFindAllSearchScope());
    return getLdapTemplate().findAll(searchRequest, dnsZoneLdapMapper)
        .filter(this::isNonExcludedDnsZone);
  }

  @Override
  public boolean exists(final String zoneName) {
    return isNonExcludedDnsZone(zoneName)
        && getLdapTemplate().exists(DnsZone.builder().name(zoneName).build(), dnsZoneLdapMapper);
  }

  @Override
  public Optional<DnsZone> findOne(final String zoneName) {
    final SearchFilter searchFilter = new SearchFilter(getProperties().getDnsZoneFindOneFilter());
    searchFilter.setParameter(0, zoneName);
    final SearchRequest searchRequest = new SearchRequest(
        getProperties().getDnsZoneBaseDn(),
        searchFilter);
    searchRequest.setSearchScope(getProperties().getDnsZoneFindOneSearchScope());
    return getLdapTemplate()
        .findOne(searchRequest, dnsZoneLdapMapper)
        .filter(this::isNonExcludedDnsZone);
  }

  @Override
  public DnsZone save(final String zoneName) {
    if (isExcludedDnsZone(zoneName)) {
      throw ServiceException.badRequest(
          "Zone name is not allowed.",
          "org.bremersee:dc-con-app:bc02abb3-f5d9-4a95-9761-98def37d12a9");
    }
    return findOne(zoneName)
        .orElseGet(() -> execDnsZoneCmd(
            "zonecreate",
            zoneName, response -> findOne(zoneName)
                .orElseThrow(() -> ServiceException.internalServerError(
                    "msg=[Saving dns zone failed.] "
                        + CommandExecutorResponse.toExceptionMessage(response),
                    "org.bremersee:dc-con-app:905a21c0-0ab9-4562-a83f-b849dbbea6c0"))));
  }

  @Override
  public boolean delete(final String zoneName) {
    if (exists(zoneName)) {
      execDnsZoneCmd(
          "zonedelete",
          zoneName,
          (CommandExecutorResponseValidator) response -> {
            if (exists(zoneName)) {
              throw ServiceException.internalServerError(
                  "msg=[Deleting dns zone failed.] " + toExceptionMessage(response),
                  "org.bremersee:dc-con-app:346a54dd-c882-4c41-8503-7089928aeaa3");
            }
          });
      return true;
    }
    return false;
  }

  private <T> T execDnsZoneCmd(
      final String dnsCommand,
      final String zoneName,
      final CommandExecutorResponseParser<T> parser) {

    kinit();
    final List<String> commands = new ArrayList<>();
    sudo(commands);
    commands.add(getProperties().getSambaToolBinary());
    commands.add("dns");
    commands.add(dnsCommand);
    commands.add(getProperties().getNameServerHost());
    commands.add(zoneName);
    auth(commands);
    return CommandExecutor.exec(
        commands, null, getProperties().getSambaToolExecDir(), parser);
  }


}
