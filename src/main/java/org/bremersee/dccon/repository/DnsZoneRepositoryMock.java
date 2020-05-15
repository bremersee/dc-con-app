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

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DnsZone;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * The dns zone repository mock.
 *
 * @author Christian Bremer
 */
@Profile("!ldap")
@Component
@Slf4j
public class DnsZoneRepositoryMock implements DnsZoneRepository {

  private static final String DISTINGUISHED_NAME_TEMPLATE = "DC=%s,"
      + "CN=MicrosoftDNS,DC=DomainDnsZones,DC=samdom,DC=example,DC=org";

  private final Map<String, DnsZone> repo = new ConcurrentHashMap<>();

  @Getter(AccessLevel.PACKAGE)
  private final DomainControllerProperties properties;

  /**
   * Instantiates a new dns zone repository mock.
   *
   * @param properties the properties
   */
  public DnsZoneRepositoryMock(
      DomainControllerProperties properties) {
    this.properties = properties;
  }

  /**
   * Init.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void init() {
    log.warn("\n"
        + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n"
        + "!! MOCK is running:  DnsZoneRepository                                              !!\n"
        + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
    );
  }

  @Override
  public boolean isDnsReverseZone(String dnsZoneName) {
    return getProperties().isReverseZone(dnsZoneName);
  }

  @Override
  public Stream<DnsZone> findAll() {
    return repo.values().stream()
        .map(this::build);
  }

  @Override
  public boolean exists(@NotNull String zoneName) {
    return repo.containsKey(zoneName.toLowerCase());
  }

  @Override
  public Optional<DnsZone> findOne(@NotNull String zoneName) {
    return Optional.ofNullable(build(repo.get(zoneName.toLowerCase())));
  }

  @Override
  public DnsZone save(@NotNull String zoneName) {
    return build(repo.computeIfAbsent(zoneName.toLowerCase(), key -> DnsZone.builder()
        .created(OffsetDateTime.now())
        .modified(OffsetDateTime.now())
        .name(zoneName)
        .build()));
  }

  @Override
  public boolean delete(@NotNull String zoneName) {
    return repo.remove(zoneName.toLowerCase()) != null;
  }

  private DnsZone build(DnsZone dnsZone) {
    return dnsZone == null ? null : dnsZone.toBuilder()
        .defaultZone(dnsZone.getName().equalsIgnoreCase(getProperties().getDefaultZone()))
        .reverseZone(getProperties().isReverseZone(dnsZone.getName()))
        .distinguishedName(String.format(DISTINGUISHED_NAME_TEMPLATE, dnsZone.getName()))
        .build();
  }

}
