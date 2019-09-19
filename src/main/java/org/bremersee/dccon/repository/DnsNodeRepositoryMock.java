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

import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.model.DnsNode;
import org.bremersee.dccon.model.DnsPair;
import org.bremersee.dccon.model.DnsRecord;
import org.bremersee.dccon.model.UnknownFilter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * The dns node repository mock.
 *
 * @author Christian Bremer
 */
@Profile("!ldap")
@Component
@Slf4j
public class DnsNodeRepositoryMock implements DnsNodeRepository {

  /**
   * Init.
   */
  @PostConstruct
  public void init() {
    log.warn("\n"
        + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n"
        + "!! MOCK is running:  DnsNodeRepository                                              !!\n"
        + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
    );
  }

  @Override
  public Stream<DnsNode> findAll(String zoneName, UnknownFilter unknownFilter) {
    return Stream.empty();
  }

  @Override
  public boolean exists(String zoneName, String nodeName, UnknownFilter unknownFilter) {
    return false;
  }

  @Override
  public Optional<DnsNode> findOne(String zoneName, String nodeName, UnknownFilter unknownFilter) {
    return Optional.empty();
  }

  @Override
  public Optional<DnsPair> findCorrelatedDnsNode(String zoneName, DnsRecord record) {
    return Optional.empty();
  }

  @Override
  public Optional<DnsNode> save(String zoneName, DnsNode dnsNode) {
    return Optional.empty();
  }

  @Override
  public boolean delete(String zoneName, DnsNode node) {
    return false;
  }
}
