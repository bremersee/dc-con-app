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

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNullElse;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DnsEntry;
import org.bremersee.dccon.model.DnsEntryType;
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.model.DnsZoneType;
import org.bremersee.dccon.repository.cli.parser.DnsEntriesParser;
import org.bremersee.dccon.repository.cli.parser.DnsEntryAddValidator;
import org.bremersee.dccon.repository.cli.parser.DnsEntryDeleteValidator;
import org.bremersee.dccon.repository.cli.parser.DnsEntryUpdateValidator;
import org.bremersee.dccon.repository.cli.parser.DnsZoneCreateValidator;
import org.bremersee.dccon.repository.cli.parser.DnsZoneDeleteValidator;
import org.bremersee.dccon.repository.cli.parser.DnsZoneListParser;
import org.bremersee.dccon.repository.cli.parser.DnsZoneParser;
import org.bremersee.dccon.repository.mapper.CommonAttributesLdapMapper;
import org.bremersee.exception.ServiceException;
import org.bremersee.ldaptive.LdaptiveTemplate;
import org.ldaptive.LdapEntry;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
import org.ldaptive.dn.NameValue;
import org.ldaptive.dn.RDn;
import org.ldaptive.filter.AndFilter;
import org.ldaptive.filter.EqualityFilter;
import org.ldaptive.filter.Filter;
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
    return executeAndGet(commands, DnsZoneListParser.defaultParser());
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
    return Optional.ofNullable(executeAndGet(commands, DnsZoneParser.defaultParser()))
        .map(zone -> {
          CommonAttributesLdapMapper.mapCommonAttributes(
              getLdapTemplate(),
              new Dn(zone.getDistinguishedName()),
              zone);
          return zone;
        });
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
    execute(commands, new DnsZoneCreateValidator(zoneName));
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
    execute(commands, new DnsZoneDeleteValidator(zoneName));
  }


  @Override
  public Stream<DnsEntry> findDnsEntries(String zoneName, String name, DnsEntryType type) {
    log.debug("findDnsEntriesByZoneName {} {} {}", zoneName, name, type);

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
        zoneName,
        name,
        type.name()
    );
    return executeAndGet(
        commands,
        DnsEntriesParser.defaultParser(name));
  }

  private Optional<LdapEntry> findDnsLdapEntryWithConflict(Dn zoneDn, DnsEntry dnsEntry) {
    if (isNull(dnsEntry.getObjectGuid())) {
      return Optional.empty();
    }
    Filter filter = new AndFilter(
        new EqualityFilter(LDAP_OBJECT_CLASS, LDAP_OBJECT_CLASS_DNS_NODE),
        new EqualityFilter(LDAP_OBJECT_GUID, dnsEntry.getObjectGuid()));
    SearchRequest searchRequest = SearchRequest.builder()
        .dn(zoneDn.format())
        .filter(filter)
        .scope(SearchScope.ONELEVEL)
        .returnAttributes(LDAP_WHEN_CREATED, LDAP_WHEN_CHANGED, "dNSTombstoned")
        .build();
    return getLdapTemplate().findOne(searchRequest)
        .filter(getIgnoredEntryFilter());
  }

  @Override
  public Optional<DnsEntry> findDnsEntryWithConflict(Dn zoneDn, DnsEntry dnsEntry) {
    return findDnsLdapEntryWithConflict(zoneDn, dnsEntry)
        .map(ldapEntry -> {
          CommonAttributesLdapMapper.mapCommonAttributes(ldapEntry, dnsEntry);
          log.debug("Dns entry with conflict found: {}", dnsEntry);
          return dnsEntry;
        });
  }

  public boolean deleteDnsEntryWithConflict(Dn zoneDn, DnsEntry dnsEntry) {
    // TODO
    return findDnsLdapEntryWithConflict(zoneDn, dnsEntry)
        .map(ldapEntry -> {
          return true;
        })
        .orElse(false);
  }

  @Override
  public DnsEntry setCommonAttributes(Dn zoneDn, DnsEntry entry) {
    Dn dn = new Dn(new RDn(new NameValue("DC", entry.getName())));
    dn.add(zoneDn);
    CommonAttributesLdapMapper.mapCommonAttributes(getLdapTemplate(), dn, entry);
    return entry;
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
        entry.getType().getToSambaToolValueTransformer().apply(entry.getValue())
    );
    execute(commands, DnsEntryAddValidator.getInstance());
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
        entry.getType().getToSambaToolValueTransformer().apply(entry.getValue()),
        entry.getType().getToSambaToolValueTransformer().apply(newValue)
    );
    execute(commands, DnsEntryUpdateValidator.getInstance());
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
        entry.getType().getToSambaToolValueTransformer().apply(entry.getValue())
    );
    execute(commands, DnsEntryDeleteValidator.getInstance());
  }

}
