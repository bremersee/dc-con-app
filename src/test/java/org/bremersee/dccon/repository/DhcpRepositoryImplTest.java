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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DhcpLease;
import org.bremersee.dccon.repository.cli.CommandExecutorResponse;
import org.bremersee.dccon.repository.cli.DhcpLeaseParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The dhcp repository impl test.
 *
 * @author Christian Bremer
 */
class DhcpRepositoryImplTest {

  private static DhcpRepository repository;

  /**
   * Sets up.
   */
  @BeforeAll
  static void setUp() {
    String line0 = "MAC b8:xx:xx:xx:xx:xx "
        + "IP 192.168.1.109 "
        + "HOSTNAME ukelei "
        + "BEGIN 2019-08-18 11:20:33 "
        + "END 2019-08-18 11:50:33 "
        + "MANUFACTURER Apple, Inc."
        + "\n";
    String line1 = "MAC ac:xx:xx:xx:xx:yy "
        + "IP 192.168.1.188 "
        + "HOSTNAME -NA- "
        + "BEGIN 2019-08-18 11:25:48 "
        + "END 2019-08-18 11:55:48 "
        + "MANUFACTURER Super Micro Computer, Inc."
        + "\n";
    String lines = line0 + line1;
    DhcpLeaseParser parser = DhcpLeaseParser.defaultParser();
    DomainControllerProperties properties = new DomainControllerProperties();
    DhcpRepositoryImpl repo = new DhcpRepositoryImpl(properties);
    repo.setParser(parser);
    repo = spy(repo);
    when(repo.find(anyBoolean()))
        .thenReturn(parser.parse(new CommandExecutorResponse(lines, null)));
    repository = repo;
  }

  /**
   * Find all.
   */
  @Test
  void findAll() {
    List<DhcpLease> actual = repository.findAll();
    assertNotNull(actual);
    assertTrue(actual.stream().anyMatch(lease -> lease.getIp().equals("192.168.1.188")));
  }

  /**
   * Find active by ip.
   */
  @Test
  void findActiveByIp() {
    Map<String, DhcpLease> actual = repository.findActiveByIp();
    assertNotNull(actual);
    assertNotNull(actual.get("192.168.1.109"));
    assertEquals("ukelei", actual.get("192.168.1.109").getHostname());
  }

  /**
   * Find active by host name.
   */
  @Test
  void findActiveByHostName() {
    Map<String, DhcpLease> actual = repository.findActiveByHostName();
    assertNotNull(actual);
    assertNotNull(actual.get("UKELEI"));
    assertEquals("192.168.1.109", actual.get("UKELEI").getIp());
  }

}