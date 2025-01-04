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

import static org.springframework.util.ObjectUtils.isEmpty;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import lombok.Getter;
import org.bremersee.dccon.model.CommonAttributes;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.model.OrganizationalUnit;
import org.ldaptive.dn.Dn;
import org.ldaptive.dn.NameValue;
import org.ldaptive.dn.RDn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * The type RepositoryMockStore.
 *
 * @author Christian Bremer
 */
@Profile("mock")
@Component
@Getter
public class RepositoryMockStore {

  private final List<OrganizationalUnit> ouRepo = new CopyOnWriteArrayList<>();

  private final Map<String, DomainGroup> groupRepo = new ConcurrentHashMap<>();

  private final Map<String, DomainUser> userRepo = new ConcurrentHashMap<>();

  private final Map<String, byte[]> avatarRepo = new ConcurrentHashMap<>();

  public Stream<String> findSamAccountNames() {
    Stream<String> stream = groupRepo.keySet().stream();
    return Stream.concat(stream, userRepo.keySet().stream());
  }

  public Stream<? extends CommonAttributes> findCommonAttributes() {
    Stream<? extends CommonAttributes> stream = groupRepo.values().stream();
    return Stream.concat(stream, userRepo.values().stream());
  }

  public static void updateCommonAttributes(CommonAttributes object, Dn parentDn, String rdnName, String rdnValue) {
    Dn dn = new Dn(new RDn(new NameValue(rdnName, rdnValue)));
    dn.add(parentDn);
    object.setDistinguishedName(dn.format());
    if (isEmpty(object.getCreated())) {
      object.setCreated(OffsetDateTime.now());
    }
    object.setModified(OffsetDateTime.now());
  }
}
