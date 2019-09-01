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

package org.bremersee.dccon.business;

import java.util.ArrayList;
import java.util.List;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DhcpLease;
import org.springframework.stereotype.Component;

/**
 * The dhcp lease list implementation.
 *
 * @author Christian Bremer
 */
@Component
public class DhcpLeaseListCommandExecutor implements DhcpLeaseList {

  private final DomainControllerProperties properties;

  private final DhcpLeaseListParser dhcpLeaseListParser;

  public DhcpLeaseListCommandExecutor(
      DomainControllerProperties properties,
      DhcpLeaseListParser dhcpLeaseListParser) {
    this.properties = properties;
    this.dhcpLeaseListParser = dhcpLeaseListParser;
  }

  @Override
  public List<DhcpLease> getDhcpLeases(Boolean all) {
    final List<String> commands = new ArrayList<>();
    if (properties.isUsingSudo()) {
      commands.add(properties.getSudoBinary());
    }
    commands.add(properties.getDhcpLeaseListBinary());
    commands.add("--parsable");
    if (Boolean.TRUE.equals(all)) {
      commands.add("--all");
    }
    final CommandExecutorResponse response = CommandExecutor.exec(
        commands, properties.getDhcpLeaseListExecDir());
    return dhcpLeaseListParser.parseDhcpLeaseList(response);
  }
}
