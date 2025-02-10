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
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.comparator.spring.mapper.SortMapper;
import org.bremersee.dccon.ErrorCode;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.OrganizationalUnit;
import org.bremersee.dccon.repository.OrganizationalUnitRepository;
import org.bremersee.pagebuilder.PageBuilder;
import org.ldaptive.dn.Dn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  private final SortMapper sortMapper;

  private final OrganizationalUnitRepository repository;

  @Getter
  private final OrganizationalUnit base;

  public OrganizationalUnitServiceImpl(
      DomainControllerProperties properties,
      SortMapper sortMapper,
      OrganizationalUnitRepository repository) {
    this.properties = properties;
    this.sortMapper = sortMapper;
    this.repository = repository;
    this.base = new OrganizationalUnit();
    this.base.setDn(properties.getBaseDn());
    this.base.setCreated(OffsetDateTime.now());
    this.base.setModified(OffsetDateTime.now());
    this.base.setName("Base");
    this.base.setDescription("Base of Active Directory");
    this.base.setSystemOu(true);
  }

  boolean contains(OrganizationalUnit ou, String query) {
    if (!isEmpty(ou.getName()) && ou.getName().toLowerCase().contains(query)) {
      return true;
    }
    return !isEmpty(ou.getDescription()) && ou.getDescription().toLowerCase().contains(query);
  }

  @Override
  public Page<OrganizationalUnit> getOrganizationalUnits(Pageable pageable, String query) {
    Stream<OrganizationalUnit> ous = getOrganizationalUnitsWithSystemOus();
    if (isEmpty(query) || query.length() <= 2) {
      return new PageBuilder<OrganizationalUnit, OrganizationalUnit>()
          .sourceEntries(ous)
          .pageable(sortMapper.applyDefaults(pageable, null, true, null))
          .build();
    }
    String lowerQuery = query.toLowerCase();
    return new PageBuilder<OrganizationalUnit, OrganizationalUnit>()
        .sourceEntries(ous.filter(ou -> contains(ou, lowerQuery)))
        .pageable(sortMapper.applyDefaults(pageable, null, true, null))
        .build();
  }

  @Override
  public Stream<OrganizationalUnit> getOrganizationalUnits() {
    return repository.findAll().sorted(Comparator.comparing(OrganizationalUnit::getNameTree));
  }

  @Override
  public Stream<OrganizationalUnit> getOrganizationalUnitsWithBase() {
    return Stream.concat(
        Stream.of(base),
        repository.findAll()
            .sorted(Comparator.comparing(OrganizationalUnit::getNameTree)));
  }

  @Override
  public Stream<OrganizationalUnit> getOrganizationalUnitsWithSystemOus() {
    return repository.findAllWithSystemOus()
        .sorted(Comparator.comparing(OrganizationalUnit::getNameTree));
  }

  @Override
  public Stream<OrganizationalUnit> getOrganizationalUnitsWithSystemOusAndBase() {
    return Stream.concat(Stream.of(base), getOrganizationalUnitsWithSystemOus());
  }

  @Override
  public Optional<OrganizationalUnit> getOrganizationalUnit(Dn ou) {
    return Optional.ofNullable(ou)
        .filter(dn -> !dn.isEmpty())
        .flatMap(repository::findOne)
        .or(() -> Optional.ofNullable(ou)
            .filter(dn -> dn.isSame(properties.getBaseDn()))
            .map(baseDn -> base));
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

  @Override
  public boolean hasChildren(Dn ou) {
    return repository.hasChildren(ou);
  }

  @Override
  public OrganizationalUnit add(OrganizationalUnit organizationalUnit, Dn parentOu) {
    return repository.add(organizationalUnit, parentOu);
  }

  @Override
  public OrganizationalUnit update(OrganizationalUnit organizationalUnit, Dn newParentOu) {
    return repository.update(organizationalUnit, newParentOu);
  }

  @Override
  public boolean delete(Dn ou) {
    return repository.delete(ou);
  }

}
