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

package org.bremersee.dccon.service;

import static org.springframework.util.ObjectUtils.isEmpty;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.ErrorCode;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.OrganizationalUnit;
import org.bremersee.dccon.repository.OrganizationalUnitRepository;
import org.ldaptive.dn.Dn;
import org.springframework.stereotype.Service;

/**
 * The type OrganizationalUnitServiceImpl.
 *
 * @author Christian Bremer
 */
@Service
@Slf4j
public class OrganizationalUnitServiceImpl implements OrganizationalUnitService, ErrorCode {

  private final DomainControllerProperties properties;

  private final OrganizationalUnitRepository repository;

  @Getter
  private final OrganizationalUnit base;

  public OrganizationalUnitServiceImpl(
      DomainControllerProperties properties,
      OrganizationalUnitRepository repository) {
    this.properties = properties;
    this.repository = repository;
    this.base = OrganizationalUnit.builder()
        .distinguishedName(properties.getBaseDn().format())
        .created(OffsetDateTime.now())
        .modified(OffsetDateTime.now())
        .name("Base")
        .description("Base of Active Directory")
        .systemOu(true)
        .build();
  }

  OrganizationalUnit withFormattedDn(OrganizationalUnit organizationalUnit) {
    if (isEmpty(organizationalUnit) || isEmpty(organizationalUnit.getDistinguishedName())) {
      return organizationalUnit;
    }
    return organizationalUnit.toBuilder()
        .distinguishedName(new Dn(organizationalUnit.getDistinguishedName()).format())
        .build();
  }

  @Override
  public Stream<OrganizationalUnit> getOrganizationalUnits() {
    return repository.findAll().map(this::withFormattedDn);
  }

  @Override
  public Stream<OrganizationalUnit> getOrganizationalUnitsWithBase() {
    return Stream.concat(getOrganizationalUnits(), Stream.of(base));
  }

  @Override
  public Optional<OrganizationalUnit> getOrganizationalUnit(Dn ou) {
    return Optional.ofNullable(ou)
        .filter(dn -> !dn.isEmpty())
        .flatMap(repository::findOne)
        .or(() -> Optional.ofNullable(ou)
            .filter(dn -> dn.isSame(properties.getBaseDn()))
            .map(baseDn -> base))
        .map(this::withFormattedDn);
  }

  @Override
  public boolean organisationUnitExists(Dn ou) {
    if (isEmpty(ou) || ou.isEmpty()) {
      return false;
    }
    if (properties.getBaseDn().isSame(ou)) {
      return true;
    }
    return repository.exists(ou);
  }

}
