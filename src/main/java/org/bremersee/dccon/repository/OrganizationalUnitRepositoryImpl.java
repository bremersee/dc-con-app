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

import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.ErrorCode;
import org.bremersee.dccon.config.DomainComputerProperties;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.config.DomainUserProperties;
import org.bremersee.dccon.model.OrganizationalUnit;
import org.bremersee.dccon.repository.automock.MockComponent;
import org.bremersee.dccon.repository.automock.ProfileRequired;
import org.bremersee.dccon.repository.cli.CommandExecutor;
import org.bremersee.dccon.repository.cli.CommandExecutorResponse;
import org.bremersee.dccon.repository.mapper.OrganizationalUnitLdapMapper;
import org.bremersee.exception.ServiceException;
import org.bremersee.ldaptive.LdaptiveEntryMapper;
import org.bremersee.ldaptive.LdaptiveTemplate;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
import org.ldaptive.dn.NameValue;
import org.ldaptive.dn.RDn;
import org.ldaptive.filter.EqualityFilter;
import org.ldaptive.filter.Filter;
import org.ldaptive.filter.OrFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * The type OrganizationalUnitRepositoryImpl.
 *
 * @author Christian Bremer
 */
@Primary
@Component("organizationalUnitRepository")
@ProfileRequired("ldap")
@MockComponent(value = OrganizationalUnitRepositoryMock.class,
    methodsOf = OrganizationalUnitRepository.class)
@Slf4j
public class OrganizationalUnitRepositoryImpl extends AbstractOrganizationalUnitRepository {

  private LdaptiveEntryMapper<OrganizationalUnit> ouLdapMapper;

  OrganizationalUnitRepositoryImpl(DomainControllerProperties properties,
      ObjectProvider<LdaptiveTemplate> ldapTemplateProvider) {
    super(properties, ldapTemplateProvider.getIfAvailable());
    ouLdapMapper = new OrganizationalUnitLdapMapper(properties);
  }

  @Autowired(required = false)
  public void setOuLdapMapper(LdaptiveEntryMapper<OrganizationalUnit> ouLdapMapper) {
    if (!isEmpty(ouLdapMapper)) {
      this.ouLdapMapper = ouLdapMapper;
    }
  }

  @Override
  Filter objectClassFilter() {
    return new OrFilter(
        new EqualityFilter(LDAP_OBJECT_CLASS, getObjectClassValue()),
        new EqualityFilter(LDAP_DN,
            getProperties().getBaseDn(DomainUserProperties.DEFAULT_OU).format()),
        new EqualityFilter(LDAP_DN,
            getProperties().getBaseDn(DomainComputerProperties.DEFAULT_OU).format()));
  }

  @Override
  public Stream<OrganizationalUnit> findAll() {
    SearchRequest computersSearchRequest = SearchRequest
        .objectScopeSearchRequest(
            getProperties().getBaseDn(DomainComputerProperties.DEFAULT_OU).format(),
            getReturnAttributes());
    Stream<OrganizationalUnit> stream = getLdapTemplate()
        .findOne(computersSearchRequest, ouLdapMapper)
        .stream();

    SearchRequest usersSearchRequest = SearchRequest
        .objectScopeSearchRequest(
            getProperties().getBaseDn(DomainUserProperties.DEFAULT_OU).format(),
            getReturnAttributes());
    stream = Stream.concat(
        stream,
        getLdapTemplate().findOne(usersSearchRequest, ouLdapMapper).stream());

    SearchRequest searchRequest = searchAllRequest(
        null,
        new EqualityFilter(LDAP_OBJECT_CLASS, LDAP_OBJECT_CLASS_OU),
        SearchScope.SUBTREE,
        getReturnAttributes());
    return Stream.concat(
            stream,
            getLdapTemplate().findAll(searchRequest, ouLdapMapper))
        .filter(getIgnoredObjectFilter());
  }

  @Override
  public Optional<OrganizationalUnit> findOne(Dn ou) {
    log.debug("findOne({})", ou);
    if (ou.isEmpty()) {
      return Optional.empty();
    }
    String dn = getProperties().getBaseDn(ou).format();
    log.debug("findOne, dn = {}", dn);
    new EqualityFilter(LDAP_OBJECT_CLASS, getObjectClassValue());
    SearchRequest searchRequest = searchOneRequest(dn);
    return getLdapTemplate()
        .findOne(searchRequest, ouLdapMapper);
  }

  @Override
  public boolean exists(Dn ou) {
    return findOne(ou).isPresent();
  }

  @ProfileRequired({"cli", "ldap"})
  @Override
  public OrganizationalUnit add(OrganizationalUnit organizationalUnit, Dn parentOu) {
    if (isEmpty(organizationalUnit.getName())) {
      throw ServiceException.badRequest(
          "Name of organizational unit is required.", EC_OU_NAME_REQUIRED);
    }
    if (organizationalUnit.getName().contains(",")) {
      throw ServiceException.badRequest(
          "Name of organizational unit contains illegal characters.", EC_ILLEGAL_OU_NAME);
    }
    Dn dn = new Dn(new RDn(new NameValue(LDAP_OU, organizationalUnit.getName())));
    if (!isEmpty(parentOu) && !parentOu.isEmpty()) {
      dn.add(validateOu(parentOu));
    }
    if (exists(dn)) {
      throw ServiceException.alreadyExistsWithErrorCode(
          OrganizationalUnit.class.getSimpleName(),
          organizationalUnit.getName(),
          EC_OU_ALREADY_EXISTS);
    }
    kinit();
    final List<String> commands = new ArrayList<>();
    ssh(commands);
    sudo(commands);
    commands.add(getProperties().getSambaToolBinary());
    commands.add("ou");
    commands.add("add");
    commands.add(quote(dn.format()));
    if (!isEmpty(organizationalUnit.getDescription())) {
      commands.add("--description=" + quote(organizationalUnit.getDescription()));
    }
    auth(commands);
    return CommandExecutor.exec(
        commands,
        null,
        getProperties().getSambaToolExecDir(),
        response -> findOne(dn)
            .orElseThrow(() -> ServiceException
                .internalServerError(String.format("Adding organization unit '%s' failed: %s",
                        dn.format(), CommandExecutorResponse.toExceptionMessage(response)),
                    ErrorCode.EC_ADDING_OU_FAILED)));
  }

  @ProfileRequired({"cli", "ldap"})
  @Override
  public OrganizationalUnit update(
      OrganizationalUnit organizationalUnit,
      Dn newParentOu) {

    log.debug("update({}, {})", organizationalUnit.getName(), newParentOu);

    if (isEmpty(organizationalUnit.getName())) {
      throw ServiceException.badRequest(
          "Name of organizational unit is required.", EC_OU_NAME_REQUIRED);
    }
    if (organizationalUnit.getName().contains(",")) {
      throw ServiceException.badRequest(
          "Name of organizational unit contains illegal characters.", EC_ILLEGAL_OU_NAME);
    }

    OrganizationalUnit existing = findOne(new Dn(organizationalUnit.getDistinguishedName()))
        .orElseThrow(() -> ServiceException.notFoundWithErrorCode(
            OrganizationalUnit.class.getSimpleName(),
            organizationalUnit.getDistinguishedName(),
            EC_OU_NOT_FOUND));

    Dn existingDn = new Dn(existing.getDistinguishedName());
    log.debug("Existing ou dn: {}", existingDn);
    Dn wantedDn = new Dn(new RDn(new NameValue(LDAP_OU, organizationalUnit.getName())));
    if (isEmpty(newParentOu) || newParentOu.isEmpty()) {
      wantedDn.add(existingDn.getParent());
    } else {
      wantedDn.add(newParentOu);
    }
    log.debug("Wanted ou dn:   {}", wantedDn);
    if (!existingDn.isSame(wantedDn)) {
      if (existing.getSystemOu()) {
        throw ServiceException.badRequest(
            "Organizational unit is a critical system object. Moving and renaming is permitted.",
            EC_ILLEGAL_SYSTEM_ENTITY_OPERATION);
      }
      if (exists(wantedDn)) {
        throw ServiceException.alreadyExistsWithErrorCode(
            OrganizationalUnit.class.getSimpleName(),
            organizationalUnit.getName(),
            EC_OU_ALREADY_EXISTS);
      }
    }

    Dn currentDn = new Dn(existing.getDistinguishedName());
    String tmpName = null;
    if (!existing.getName().equalsIgnoreCase(organizationalUnit.getName())) {
      tmpName = UUID.randomUUID().toString();
      currentDn = rename(currentDn, tmpName);
    }
    if (!existingDn.getParent().isSame(wantedDn.getParent())) {
      currentDn = move(currentDn, wantedDn.getParent());
    }
    if (!isEmpty(tmpName)) {
      currentDn = rename(currentDn, organizationalUnit.getName());
    }
    organizationalUnit.setDistinguishedName(currentDn.format());
    return getLdapTemplate().save(organizationalUnit, ouLdapMapper);
  }

  Dn move(Dn oldDn, Dn newParentOu) {
    Dn newDn = new Dn(new RDn(
        new NameValue(LDAP_OU, oldDn.getRDn().getNameValue().getStringValue())));
    newDn.add(newParentOu);
    kinit();
    List<String> commands = new ArrayList<>();
    ssh(commands);
    sudo(commands);
    commands.add(getProperties().getSambaToolBinary());
    commands.add("ou");
    commands.add("move");
    commands.add(quote(oldDn.format()));
    commands.add(quote(newParentOu.format()));
    auth(commands);
    return CommandExecutor.exec(
        commands,
        null,
        getProperties().getSambaToolExecDir(),
        response -> {
          return findOne(newDn)
              .map(OrganizationalUnit::getDistinguishedName)
              .map(Dn::new)
              .orElseThrow(() -> ServiceException.internalServerError(String.format(
                      "Moving organization unit '%s' to '%s' failed: %s",
                      oldDn.format(), newParentOu.format(),
                      CommandExecutorResponse.toExceptionMessage(response)),
                  ErrorCode.EC_UPDATING_OU_FAILED));
        });
  }

  Dn rename(Dn ou, String newName) {
    Dn newDn = new Dn(new RDn(new NameValue(LDAP_OU, newName)));
    newDn.add(ou.getParent());
    kinit();
    List<String> commands = new ArrayList<>();
    ssh(commands);
    sudo(commands);
    commands.add(getProperties().getSambaToolBinary());
    commands.add("ou");
    commands.add("rename");
    commands.add(quote(ou.format()));
    commands.add(quote(newDn.format()));
    auth(commands);
    return CommandExecutor.exec(
        commands,
        null,
        getProperties().getSambaToolExecDir(),
        response -> {
          return findOne(newDn)
              .map(OrganizationalUnit::getDistinguishedName)
              .map(Dn::new)
              .orElseThrow(() -> ServiceException.internalServerError(String.format(
                      "Renaming organization unit '%s' to '%s' failed: %s",
                      ou.format(), newName,
                      CommandExecutorResponse.toExceptionMessage(response)),
                  ErrorCode.EC_UPDATING_OU_FAILED));
        });
  }

  @ProfileRequired({"cli", "ldap"})
  @Override
  public boolean delete(Dn ou) {
    return findOne(ou)
        .filter(this::isDeletable)
        .map(o -> doDelete(ou))
        .orElse(false);
  }

  boolean isDeletable(OrganizationalUnit o) {
    return !o.getSystemOu();
  }

  boolean doDelete(Dn ou) {
    kinit();
    List<String> commands = new ArrayList<>();
    ssh(commands);
    sudo(commands);
    commands.add(getProperties().getSambaToolBinary());
    commands.add("ou");
    commands.add("delete");
    commands.add(quote(ou.format()));
    auth(commands);
    return CommandExecutor.exec(
        commands,
        null,
        getProperties().getSambaToolExecDir(),
        response -> {
          if (exists(ou)) {
            throw ServiceException
                .internalServerError(String.format("Deleting organization unit '%s' failed: %s",
                        ou.format(), CommandExecutorResponse.toExceptionMessage(response)),
                    ErrorCode.EC_DELETING_OU_FAILED);
          }
          return true;
        });
  }

}
