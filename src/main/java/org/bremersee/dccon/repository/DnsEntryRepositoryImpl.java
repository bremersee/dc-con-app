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
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DnsEntry;
import org.bremersee.dccon.model.DnsEntryType;
import org.bremersee.dccon.repository.cli.parser.DnsEntriesParser;
import org.bremersee.dccon.repository.cli.parser.DnsEntryAddValidator;
import org.bremersee.dccon.repository.cli.parser.DnsEntryDeleteValidator;
import org.bremersee.dccon.repository.cli.parser.DnsEntryUpdateValidator;
import org.bremersee.dccon.repository.mapper.CommonAttributesLdapMapper;
import org.bremersee.exception.ServiceException;
import org.bremersee.ldaptive.LdaptiveTemplate;
import org.ldaptive.DeleteRequest;
import org.ldaptive.dn.Dn;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * The type DnsRepositoryImpl.
 *
 * @author Christian Bremer
 */
@Primary
@Component("dnsEntryRepository")
@Slf4j
public class DnsEntryRepositoryImpl extends AbstractRepository implements DnsEntryRepository {

  private final DomainRepository domainRepository;

  private final DnsZoneRepository dnsZoneRepository;

  public DnsEntryRepositoryImpl(
      DomainControllerProperties properties,
      ObjectProvider<LdaptiveTemplate> ldapTemplateProvider,
      DomainRepository domainRepository,
      DnsZoneRepository dnsZoneRepository) {
    super(properties, ldapTemplateProvider.getIfAvailable());
    this.domainRepository = domainRepository;
    this.dnsZoneRepository = dnsZoneRepository;
  }

  @Cacheable(value = "dnsEntryListCache", key = "{ #p0 }")
  @Override
  public List<DnsEntry> getDnsEntries(String zoneName) {
    log.debug("findDnsEntries({})", zoneName);

    List<String> commands = List.of(
        getProperties().getCli().getSambaToolBinary(),
        "dns",
        "query",
        domainRepository.getHostName(),
        zoneName,
        ZONE_ENTRIES_NODE_NAME,
        DnsEntryType.ALL.name()
    );
    return executeAndGet(commands, DnsEntriesParser.defaultParser(zoneName, ZONE_ENTRIES_NODE_NAME))
        .toList();
  }

  @Override
  public DnsEntry addCommonAttributes(DnsEntry entry) {
    Dn dn = new Dn("DC=" + entry.getName());
    dn.add(dnsZoneRepository.getDnsZone(entry.getZoneName()).getDn());
    CommonAttributesLdapMapper.mapCommonAttributes(getLdapTemplate(), dn, entry);
    return entry;
  }

  @CacheEvict(value = "dnsEntryListCache", allEntries = true)
  @Override
  public void addDnsEntry(DnsEntry entry) {
    log.debug("addDnsEntry({})", entry);
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
        entry.getZoneName(),
        entry.getName(),
        entry.getType().name(),
        entry.getType().getToSambaToolValueTransformer().apply(entry.getValue())
    );
    execute(commands, DnsEntryAddValidator.getInstance());
  }

  @CacheEvict(value = "dnsEntryListCache", allEntries = true)
  @Override
  public void updateDnsEntry(DnsEntry entry, String newValue) {
    log.debug("updateDnsEntry {}, {}", entry, newValue);
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
        entry.getZoneName(),
        entry.getName(),
        entry.getType().name(),
        entry.getType().getToSambaToolValueTransformer().apply(entry.getValue()),
        entry.getType().getToSambaToolValueTransformer().apply(newValue)
    );
    execute(commands, DnsEntryUpdateValidator.getInstance());
  }

  @CacheEvict(value = "dnsEntryListCache", allEntries = true)
  @Override
  public void deleteDnsEntry(DnsEntry entry) {
    if (entry.isConflict()) {
      deleteDnsEntryConflict(entry);
      return;
    }
    log.debug("deleteDnsEntry({})", entry);
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
        entry.getZoneName(),
        entry.getName(),
        entry.getType().name(),
        entry.getType().getToSambaToolValueTransformer().apply(entry.getValue())
    );
    execute(commands, DnsEntryDeleteValidator.getInstance());
  }

  private void deleteDnsEntryConflict(DnsEntry entry) {
    log.debug("deleteDnsEntryConflict({})", entry);
    Dn dn = new Dn("DC=" + entry.getName());
    dn.add(dnsZoneRepository.getDnsZone(entry.getZoneName()).getDn());
    getLdapTemplate().delete(DeleteRequest.builder()
        .dn(dn.format())
        .build());
  }

}
