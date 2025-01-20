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
import static java.util.Objects.requireNonNullElseGet;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DnsEntry;
import org.bremersee.dccon.model.DnsEntryType;
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.model.DnsZoneType;
import org.bremersee.dccon.repository.cli.CommandExecutorResponse;
import org.bremersee.dccon.repository.cli.parser.DnsEntriesParser;
import org.bremersee.dccon.repository.cli.parser.DnsZoneListParser;
import org.bremersee.dccon.repository.cli.parser.DnsZoneParser;
import org.bremersee.exception.ServiceException;
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
@Slf4j
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
  public List<String> findDnsZoneNames(DnsZoneType type) {
    DnsZoneType zoneType = requireNonNullElse(type, DnsZoneType.PRIMARY);
    log.debug("findDnsZoneNames({})", zoneType);
    List<String> commands = List.of(
        getProperties().getCli().getSambaToolBinary(),
        "dns",
        "zonelist",
        domainRepository.getHostName(),
        "--" + zoneType.getParameterValue()
    );
    List<String> zoneNames = executeAndGet(commands, DnsZoneListParser.defaultParser());
    return requireNonNullElseGet(zoneNames, List::of);
  }

  @Override
  public Optional<DnsZone> findDnsZone(String zoneName) {
    log.debug("findDnsZone {}", zoneName);
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
  public void createDnsZone(String zoneName) {
    log.debug("createDnsZone {}", zoneName);
    List<String> commands = List.of(
        getProperties().getCli().getSambaToolBinary(),
        "dns",
        "zonecreate",
        domainRepository.getHostName(),
        zoneName
    );
    executeAndLog(commands);
  }

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
    executeAndLog(commands);
  }


  @Override
  public Stream<DnsEntry> findDnsEntries(DnsZone dnsZone, String name, DnsEntryType type) {
    log.debug("findDnsEntry {} {} {}", dnsZone.getName(), name, type);

    if (!type.isQueryable()) {
      throw ServiceException.badRequest(
          String.format("Dns entry type '%s' is not supported.", type),
          EC_ILLEGAL_DNS_ENTRY_TYPE);
    }
    List<String> commands = List.of(
        getProperties().getCli().getSambaToolBinary(),
        "dns",
        "query",
        domainRepository.getHostName(),
        dnsZone.getName(),
        name,
        type.name()
    );
    LdaptiveTemplate ldapTemplate = DnsEntryType.ALL.equals(type) ? null : getLdapTemplate();
    Stream<DnsEntry> dnsEntryStream = executeAndGet(
        commands,
        DnsEntriesParser.defaultParser(name, dnsZone.getDn(), ldapTemplate));
    return requireNonNullElseGet(dnsEntryStream, Stream::empty);
  }

  @Override
  public void addDnsEntry(String zoneName, DnsEntry entry) {
    log.debug("addDnsEntry({}, {})", zoneName, entry);
    if (!entry.getType().isAddable()) {
      throw ServiceException.badRequest(
          String.format("Dns entry type '%s' is not supported.", entry.getType()),
          EC_ILLEGAL_DNS_ENTRY_TYPE);
    }
    List<String> commands = List.of(
        getProperties().getCli().getSambaToolBinary(),
        "dns",
        "add",
        domainRepository.getHostName(),
        zoneName,
        entry.getName(),
        entry.getType().name(),
        entry.getValue()
    );
    executeAndLog(commands);
  }

  @Override
  public void updateDnsEntry(String zoneName, DnsEntry entry, String newValue) {
    log.debug("updateDnsEntry {}, {}, {}", zoneName, entry, newValue);
    if (!entry.getType().isUpdatable()) {
      throw ServiceException.badRequest(
          String.format("Dns entry type '%s' is not supported.", entry.getType()),
          EC_ILLEGAL_DNS_ENTRY_TYPE);
    }
    List<String> commands = List.of(
        getProperties().getCli().getSambaToolBinary(),
        "dns",
        "update",
        domainRepository.getHostName(),
        zoneName,
        entry.getName(),
        entry.getType().name(),
        entry.getValue(),
        newValue
    );
    executeAndLog(commands);
  }

  @Override
  public void deleteDnsEntry(String zoneName, DnsEntry entry) {
    log.debug("deleteDnsEntry({}, {})", zoneName, entry);
    if (!entry.getType().isAddable()) {
      throw ServiceException.badRequest(
          String.format("Dns entry type '%s' is not supported.", entry.getType()),
          EC_ILLEGAL_DNS_ENTRY_TYPE);
    }
    List<String> commands = List.of(
        getProperties().getCli().getSambaToolBinary(),
        "dns",
        "delete",
        domainRepository.getHostName(),
        zoneName,
        entry.getName(),
        entry.getType().name(),
        entry.getValue()
    );
    executeAndLog(commands);
  }

  private void executeAndLog(List<String> commands) {
    CommandExecutorResponse response = execute(commands);
    log.debug("response = {}", response);
  }

}
