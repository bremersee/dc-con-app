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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.ErrorCode;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.OrganizationalUnit;
import org.bremersee.dccon.model.SelectOption;
import org.bremersee.dccon.repository.OrganizationalUnitRepository;
import org.ldaptive.dn.Dn;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

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

  private final Dn baseDn;

  @Getter
  private final OrganizationalUnit base;

  public OrganizationalUnitServiceImpl(
      DomainControllerProperties properties,
      OrganizationalUnitRepository repository) {
    this.properties = properties;
    this.repository = repository;
    this.baseDn = new Dn(this.properties.getBaseDn());
    this.base = OrganizationalUnit.builder()
        .distinguishedName(properties.getBaseDn())
        .created(OffsetDateTime.now())
        .modified(OffsetDateTime.now())
        .name("Base")
        .description("Base of Active Directory")
        .systemOu(true)
        .build();
  }

  List<SelectOption<OrganizationalUnit>> getOrganizationalUnitSelectors(
      Stream<OrganizationalUnit> ous, Dn ou) {
    Dn ouDn = parseDn(ou);
    return ous
        .map(organizationalUnit -> new SelectOption<>(
            new Dn(organizationalUnit.getDistinguishedName()).format(),
            organizationalUnit,
            organizationalUnit.getNameTree(),
            isSelected(organizationalUnit, ouDn),
            false,
            false))
        .sorted()
        .collect(Collectors.toList());
  }

  @Override
  public List<SelectOption<OrganizationalUnit>> getOrganizationalUnitSelectors(Dn ou) {
    log.info("===========> ou {}", ou);
    return getOrganizationalUnitSelectors(getOrganizationalUnits(), ou);
  }

  @Override
  public List<SelectOption<OrganizationalUnit>> getOrganizationalUnitSelectorsWithBase(Dn ou) {
    return getOrganizationalUnitSelectors(getOrganizationalUnitsWithBase(), ou);
  }

  @Override
  public Stream<OrganizationalUnit> getOrganizationalUnits() {
    return repository.findAll();
  }

  @Override
  public Stream<OrganizationalUnit> getOrganizationalUnitsWithBase() {
    return Stream.concat(getOrganizationalUnits(), Stream.of(base));
  }

  @Override
  public Stream<OrganizationalUnit> getOrganizationalUnitsWithBaseButWithoutSelected(Dn ou) {
    Dn ouDn = parseDn(ou);
    return getOrganizationalUnitsWithBase()
        .filter(organizationalUnit -> !isSelected(organizationalUnit, ouDn));
  }

  @Override
  public Optional<OrganizationalUnit> getOrganizationalUnit(Dn ou) {
    return repository.findOne(ou);
  }

  private Dn parseDn(Dn ou) {
    if (ObjectUtils.isEmpty(ou) || ou.isEmpty()) {
      return baseDn;
    } else if (baseDn.isAncestor(ou) || baseDn.isSame(ou)) {
      return ou;
    }
    Dn ouDn = new Dn(ou.getRDns());
    ouDn.add(baseDn);
    return ouDn;
  }

  private boolean isSelected(OrganizationalUnit organizationalUnit, Dn ouDn) {
    return ouDn.isSame(new Dn(organizationalUnit.getDistinguishedName()));
  }

}
