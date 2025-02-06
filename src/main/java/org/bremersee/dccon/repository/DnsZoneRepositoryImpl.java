/*
 * Copyright 2025 the original author or authors.
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

import static java.util.Objects.requireNonNullElse;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.model.DnsZoneType;
import org.bremersee.dccon.repository.cli.parser.DnsZoneCreateValidator;
import org.bremersee.dccon.repository.cli.parser.DnsZoneDeleteValidator;
import org.bremersee.dccon.repository.cli.parser.DnsZoneListParser;
import org.bremersee.dccon.repository.cli.parser.DnsZoneParser;
import org.bremersee.dccon.repository.mapper.CommonAttributesLdapMapper;
import org.bremersee.exception.ServiceException;
import org.bremersee.ldaptive.LdaptiveTemplate;
import org.ldaptive.dn.Dn;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * The type DnsRepositoryImpl.
 *
 * @author Christian Bremer
 */
@Primary
@Component("dnsZoneRepository")
@Slf4j
public class DnsZoneRepositoryImpl extends AbstractRepository implements DnsZoneRepository {

  private final DomainRepository domainRepository;

  public DnsZoneRepositoryImpl(
      DomainControllerProperties properties,
      ObjectProvider<LdaptiveTemplate> ldapTemplateProvider,
      DomainRepository domainRepository) {
    super(properties, ldapTemplateProvider.getIfAvailable());
    this.domainRepository = domainRepository;
  }

  @Cacheable(value = "dnsZoneListCache", key = "{ #p0 }")
  @Override
  public List<String> getDnsZoneNames(DnsZoneType zoneType) {
    log.debug("findDnsZoneNames({})", zoneType);
    List<String> commands = List.of(
        getProperties().getCli().getSambaToolBinary(),
        "dns",
        "zonelist",
        domainRepository.getHostName(),
        "--" + zoneType.getParameterValue()
    );
    return executeAndGet(commands, DnsZoneListParser.defaultParser());
  }

  @Cacheable(value = "dnsZoneCache", key = "{ #p0 }")
  @Override
  public DnsZone getDnsZone(String zoneName) {
    log.debug("findDnsZone {}", zoneName);
    return doFindDnsZone(zoneName)
        .orElseThrow(() -> ServiceException.internalServerError(
            String.format("Dns zone '%s' was not found.", zoneName),
            "todo")); // TODO
  }

  private Optional<DnsZone> doFindDnsZone(String zoneName) {
    List<String> commands = List.of(
        getProperties().getCli().getSambaToolBinary(),
        "dns",
        "zoneinfo",
        domainRepository.getHostName(),
        zoneName
    );
    return Optional.ofNullable(executeAndGet(commands, DnsZoneParser.defaultParser()))
        .map(zone -> {
          CommonAttributesLdapMapper.mapCommonAttributes(
              getLdapTemplate(),
              new Dn(zone.getDistinguishedName()),
              zone);
          return zone;
        });
  }

  @CachePut(value = "dnsZoneCache", key = "{ #result.name }")
  @Override
  public DnsZone createDnsZone(String zoneName) {
    log.debug("createDnsZone {}", zoneName);
    List<String> commands = List.of(
        getProperties().getCli().getSambaToolBinary(),
        "dns",
        "zonecreate",
        domainRepository.getHostName(),
        zoneName
    );
    execute(commands, new DnsZoneCreateValidator(zoneName));
    return doFindDnsZone(zoneName)
        .orElseThrow(() -> ServiceException.internalServerError(
            String.format("Creating dns zone '%s' failed.", zoneName),
            EC_CREATING_DNS_ZONE_FAILED));
  }

  @CacheEvict(value = "dnsZoneCache", key = "{ #p0 }")
  @Override
  public void deleteDnsZone(String zoneName) {
    log.debug("deleteDnsZone {}", zoneName);
    List<String> commands = List.of(
        getProperties().getCli().getSambaToolBinary(),
        "dns",
        "zonedelete",
        domainRepository.getHostName(),
        zoneName
    );
    execute(commands, new DnsZoneDeleteValidator(zoneName));
  }

}
