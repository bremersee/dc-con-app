/*
 * Copyright 2024 the original author or authors.
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

import static java.util.Objects.requireNonNullElseGet;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import org.bremersee.dccon.ErrorCode;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.CommonAttributes;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.model.DomainUser;
import org.ldaptive.dn.Dn;
import org.springframework.util.ObjectUtils;

/**
 * The type AbstractRepositoryMock.
 *
 * @author Christian Bremer
 */
abstract class AbstractRepositoryMock
    implements RepositoryMock, RepositoryMockConstants, ErrorCode {

  final Map<String, DomainGroup> groupRepo = new ConcurrentHashMap<>();

  final Map<String, DomainUser> userRepo = new ConcurrentHashMap<>();

  @Getter(AccessLevel.PACKAGE)
  private final DomainControllerProperties properties;

  AbstractRepositoryMock(DomainControllerProperties properties) {
    this.properties = requireNonNullElseGet(properties, DomainControllerProperties::new);
    if (ObjectUtils.isEmpty(getProperties().getBaseDn())) {
      getProperties().setBaseDn("DC=samdom,DC=example,DC=org");
    }
  }

  Stream<String> findAllNames() {
    Stream<String> stream = groupRepo.keySet().stream();
    return Stream.concat(stream, userRepo.keySet().stream());
  }

  Stream<? extends CommonAttributes> findAllEntities() {
    Stream<? extends CommonAttributes> stream = groupRepo.values().stream();
    return Stream.concat(stream, userRepo.values().stream());
  }

  void updateCommonAttributes(CommonAttributes entity, Dn ou, String name) {
    entity.setDistinguishedName(String.format("CN=%s,%s,%s", name,
        requireNonNullElseGet(ou, () -> new Dn("CN=Users")).format(), properties.getBaseDn()));
    if (ObjectUtils.isEmpty(entity.getCreated())) {
      entity.setCreated(OffsetDateTime.now());
    }
    entity.setModified(OffsetDateTime.now());
  }
}
