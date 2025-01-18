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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.model.DnsZoneEntries;
import org.bremersee.dccon.model.DnsZoneType;
import org.bremersee.dccon.repository.cli.parser.DnsZoneEntriesParser;
import org.bremersee.dccon.repository.cli.parser.DnsZoneListParser;
import org.bremersee.dccon.repository.cli.parser.DnsZoneParser;
import org.bremersee.ldaptive.LdaptiveTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * The type DnsRepositoryImpl.
 *
 * @author Christian Bremer
 */
@Primary
@Component("dnsRepository")
public class DnsRepositoryImpl extends AbstractRepository implements DnsRepository {

  private final DomainRepository domainRepository;

  public DnsRepositoryImpl(
      DomainControllerProperties properties,
      ObjectProvider<LdaptiveTemplate> ldapTemplateProvider,
      DomainRepository domainRepository) {
    super(properties, ldapTemplateProvider.getIfAvailable());
    this.domainRepository = domainRepository;
  }

  @Override
  public Stream<DnsZone> findDnsZones(DnsZoneType type) {
    DnsZoneType zoneType = Objects.requireNonNullElse(type, DnsZoneType.PRIMARY);
    List<String> commands = List.of(
        getProperties().getCli().getSambaToolBinary(),
        "dns",
        "zonelist",
        domainRepository.getHostName(),
        "--" + zoneType.getParameterName()
    );
    return executeAndGet(commands, DnsZoneListParser.defaultParser()).stream()
        .map(this::findDnsZone)
        .flatMap(Optional::stream);
  }

  @Override
  public Optional<DnsZone> findDnsZone(String zoneName) {
    List<String> commands = List.of(
        getProperties().getCli().getSambaToolBinary(),
        "dns",
        "zoneinfo",
        domainRepository.getHostName(),
        zoneName
    );
    return Optional
        .ofNullable(executeAndGet(commands, DnsZoneParser.defaultParser(getLdapTemplate())));
  }

  @Override
  public Optional<DnsZoneEntries> findDnsEntries(String zoneName) {
    return findDnsZone(zoneName)
        .map(this::getDnsZoneEntries);
  }

  private DnsZoneEntries getDnsZoneEntries(DnsZone dnsZone) {
    List<String> commands = List.of(
        getProperties().getCli().getSambaToolBinary(),
        "dns",
        "query",
        domainRepository.getHostName(),
        dnsZone.getName(),
        DnsZoneEntriesParser.ZONE_ENTRIES_NODE_NAME,
        "ALL"
    );
    return executeAndGet(
        commands,
        DnsZoneEntriesParser.defaultParser(getLdapTemplate(), dnsZone.getDn()));
  }

}
