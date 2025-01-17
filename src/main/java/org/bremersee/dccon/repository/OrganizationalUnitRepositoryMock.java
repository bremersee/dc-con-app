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

import static java.util.Objects.nonNull;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.stream.Stream;
import org.bremersee.dccon.config.DomainComputerProperties;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.config.DomainControllerProperties.DomainProperties;
import org.bremersee.dccon.config.DomainUserProperties;
import org.bremersee.dccon.model.OrganizationalUnit;
import org.bremersee.exception.ServiceException;
import org.ldaptive.dn.Dn;
import org.ldaptive.dn.NameValue;
import org.ldaptive.dn.RDn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * The organizational unit repository mock.
 *
 * @author Christian Bremer
 */
@Profile("mock")
@Component("organizationalUnitRepositoryMock")
public class OrganizationalUnitRepositoryMock extends AbstractRepository
    implements OrganizationalUnitRepository, RepositoryMock {

  private final RepositoryMockStore store;

  public OrganizationalUnitRepositoryMock(
      DomainControllerProperties properties, RepositoryMockStore store) {
    super(properties, null);
    this.store = store;
    resetData();
  }

  @Override
  public void resetData() {
    /*
    store.getOuRepo().clear();
    store.getOuRepo().add(OrganizationalUnit.builder()
        .created(OffsetDateTime.now())
        .modified(OffsetDateTime.now())
        .distinguishedName(getProperties().getBaseDn(DomainUserProperties.DEFAULT_OU).format())
        .name("Users")
        .description("Default container for upgraded user accounts")
        .systemOu(true)
        .build());
    store.getOuRepo().add(OrganizationalUnit.builder()
        .created(OffsetDateTime.now())
        .modified(OffsetDateTime.now())
        .distinguishedName(
            getProperties().getBaseDn(DomainComputerProperties.DEFAULT_OU).format())
        .name("Computers")
        .description("Default container for upgraded computer accounts")
        .systemOu(true)
        .build());
    store.getOuRepo().add(OrganizationalUnit.builder()
        .created(OffsetDateTime.now())
        .modified(OffsetDateTime.now())
        .distinguishedName(
            getProperties().getBaseDn(DomainProperties.DEFAULT_DOMAIN_CONTROLLERS_OU).format())
        .name("Domain Controllers")
        .description("Default container for domain controllers")
        .systemOu(true)
        .build());

     */
  }

  @Override
  public Stream<OrganizationalUnit> findAll() {
    return Stream.empty();
  }

  @Override
  public Stream<OrganizationalUnit> findAllWithSystemOus() {
    return store.getOuRepo().stream()
        .map(this::copy);
  }

  @Override
  public Optional<OrganizationalUnit> findOne(Dn ou) {
    if (ou.isEmpty()) {
      return Optional.empty();
    }
    return findAllWithSystemOus().filter(o -> {
          Dn dn = new Dn(o.getDistinguishedName());
          Dn baseDn = getProperties().getBaseDn();
          Dn ouDn = new Dn(ou.getRDns());
          if (baseDn.isAncestor(ouDn)) {
            ouDn.add(baseDn);
          }
          return dn.isSame(ouDn);
        })
        .findFirst()
        .map(this::copy);
  }

  @Override
  public boolean exists(Dn ou) {
    return findOne(ou).isPresent();
  }

  @Override
  public boolean hasChildren(Dn ou) {
    return false;
  }

  @Override
  public OrganizationalUnit add(OrganizationalUnit organizationalUnit, Dn parentOu) {
    Dn ou = new Dn(new RDn(new NameValue(LDAP_OU, organizationalUnit.getName())));
    if (nonNull(parentOu) && !parentOu.isEmpty()) {
      ou.add(parentOu);
    }
    if (exists(ou)) {
      throw ServiceException.alreadyExistsWithErrorCode(
          OrganizationalUnit.class.getSimpleName(),
          organizationalUnit.getName(),
          EC_OU_ALREADY_EXISTS);
    }
    if (store.getOuRepo().size() > MAX_ENTRIES) {
      throw ServiceException.internalServerError(
          "Maximum size of organizational units is exceeded.",
          EC_MAX_MOCK_DATA);
    }
    Dn dn = new Dn();
    dn.add(ou);
    dn.add(getProperties().getBaseDn());
    /*
    OrganizationalUnit newOu = organizationalUnit.toBuilder()
        .created(OffsetDateTime.now())
        .modified(OffsetDateTime.now())
        .distinguishedName(dn.format())
        .systemOu(false)
        .build();
    store.getOuRepo().add(newOu);

     */
    return null;
  }

  public OrganizationalUnit update(OrganizationalUnit organizationalUnit) {
    return Optional.ofNullable(organizationalUnit.getDistinguishedName())
        .map(Dn::new)
        .flatMap(this::findOne)
        .map(exiting -> {
          exiting.setDescription(organizationalUnit.getDescription());
          return exiting;
        })
        .map(this::copy)
        .orElseThrow(() -> ServiceException.notFoundWithErrorCode(
            OrganizationalUnit.class.getSimpleName(),
            organizationalUnit.getName(),
            EC_OU_NOT_FOUND));
  }

  @Override
  public OrganizationalUnit update(OrganizationalUnit organizationalUnit,
      Dn newParentOu) {
    return null;
  }

  @Override
  public boolean delete(Dn ou) {
    return store.getOuRepo().removeIf(o -> isDeletable(o, ou));
  }

  private boolean isDeletable(OrganizationalUnit o, Dn ou) {
    return !o.getSystemOu() && ou.isSame(new Dn(o.getDistinguishedName()));
  }

  private OrganizationalUnit copy(OrganizationalUnit ou) {
    return null; // ou.toBuilder().build();
  }
}
