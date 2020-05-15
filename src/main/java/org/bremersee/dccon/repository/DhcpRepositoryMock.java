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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.comparator.ComparatorBuilder;
import org.bremersee.dccon.model.DhcpLease;
import org.bremersee.exception.ServiceException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Component;

/**
 * The dhcp repository mock.
 *
 * @author Christian Bremer
 */
@Profile("!cli")
@Component
@Slf4j
public class DhcpRepositoryMock implements DhcpRepository {

  private static final String DHCP_LOCATION = "classpath:demo/dhcp.json";

  private final ResourceLoader resourceLoader = new DefaultResourceLoader();

  private final ObjectMapper objectMapper;

  /**
   * Instantiates a new dhcp repository mock.
   *
   * @param objectMapperBuilder the object mapper builder
   */
  public DhcpRepositoryMock(Jackson2ObjectMapperBuilder objectMapperBuilder) {
    this.objectMapper = objectMapperBuilder.build();
  }

  /**
   * Init.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void init() {
    log.warn("\n"
        + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n"
        + "!! MOCK is running:  DhcpRepository                                                 !!\n"
        + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
    );
    findAll();
  }

  @Cacheable(cacheNames = "dhcp-leases-by-ip")
  @Override
  public Map<String, DhcpLease> findActiveByIp() {
    return findActiveMap(true);
  }

  @Cacheable(cacheNames = "dhcp-leases-by-name")
  @Override
  public Map<String, DhcpLease> findActiveByHostName() {
    return findActiveMap(false);
  }

  @Override
  public List<DhcpLease> findAll() {
    try {
      return Arrays
          .stream(objectMapper.readValue(
              resourceLoader.getResource(DHCP_LOCATION).getInputStream(), DhcpLease[].class))
          .map(dhcpLease -> dhcpLease.toBuilder()
              .begin(toNow(dhcpLease.getBegin()))
              .end(toNow(dhcpLease.getEnd()))
              .build())
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw ServiceException.internalServerError("Loading demo data failed.", e);
    }
  }

  private OffsetDateTime toNow(OffsetDateTime time) {
    if (time == null) {
      return null;
    }
    long millisToAdd = System.currentTimeMillis() - time.toInstant().toEpochMilli();
    return time.plus(millisToAdd, ChronoUnit.MILLIS);
  }

  private Map<String, DhcpLease> findActiveMap(final boolean ip) {
    final List<DhcpLease> leases = findAll();
    leases.sort(ComparatorBuilder.builder()
        .fromWellKnownText("begin,desc")
        .build());
    final Map<String, DhcpLease> leaseMap = new HashMap<>();
    for (final DhcpLease lease : leases) {
      final String key = ip
          ? lease.getIp()
          : lease.getHostname().toUpperCase();
      leaseMap.putIfAbsent(key, lease);
    }
    return leaseMap;
  }

}
