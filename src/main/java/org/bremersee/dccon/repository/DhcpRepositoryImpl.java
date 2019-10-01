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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.comparator.ComparatorBuilder;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DhcpLease;
import org.bremersee.dccon.repository.cli.CommandExecutor;
import org.bremersee.dccon.repository.cli.DhcpLeaseParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * The dhcp repository implementation.
 *
 * @author Christian Bremer
 */
@Profile("cli")
@Component("dhcpRepository")
@Slf4j
public class DhcpRepositoryImpl extends AbstractRepository implements DhcpRepository {

  private DhcpLeaseParser parser;

  /**
   * Instantiates a new dhcp repository.
   *
   * @param properties the domain controller properties
   */
  public DhcpRepositoryImpl(DomainControllerProperties properties) {
    super(properties, null);
    parser = DhcpLeaseParser.defaultParser();
  }

  /**
   * Sets dhcp lease parser.
   *
   * @param parser the dhcp lease parser
   */
  @Autowired(required = false)
  public void setParser(DhcpLeaseParser parser) {
    this.parser = parser;
  }

  @Override
  public List<DhcpLease> findAll() {
    return find(true);
  }

  @Cacheable(cacheNames = "dhcp-leases-by-ip")
  public Map<String, DhcpLease> findActiveByIp() {
    return findActiveMap(true);
  }

  @Cacheable(cacheNames = "dhcp-leases-by-name")
  public Map<String, DhcpLease> findActiveByHostName() {
    return findActiveMap(false);
  }

  private Map<String, DhcpLease> findActiveMap(boolean ip) {
    final List<DhcpLease> leases = find(false);
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

  private List<DhcpLease> find(final boolean all) {
    final List<String> commands = new ArrayList<>();
    sudo(commands);
    commands.add(getProperties().getDhcpLeaseListBinary());
    commands.add("--parsable");
    if (all) {
      commands.add("--all");
    }
    return CommandExecutor.exec(
        commands,
        null,
        getProperties().getDhcpLeaseListExecDir(),
        parser);
  }

}
