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

package org.bremersee.dccon.service;

import static org.bremersee.comparator.spring.mapper.SortMapper.applyDefaults;

import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.model.DomainGroupMembers;
import org.bremersee.dccon.repository.DomainGroupRepository;
import org.bremersee.dccon.service.validator.DomainGroupValidator;
import org.bremersee.pagebuilder.PageBuilder;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * The domain group service.
 *
 * @author Christian Bremer
 */
@Component("domainGroupService")
@Slf4j
public class DomainGroupServiceImpl implements DomainGroupService {

  private final DomainGroupRepository domainGroupRepository;

  private DomainGroupValidator domainGroupValidator;

  /**
   * Instantiates a new domain group service.
   *
   * @param properties the properties
   * @param domainGroupRepository the domain group repository
   */
  public DomainGroupServiceImpl(
      final DomainControllerProperties properties,
      final DomainGroupValidator domainGroupValidator,
      final DomainGroupRepository domainGroupRepository) {
    this.domainGroupRepository = domainGroupRepository;
    this.domainGroupValidator = domainGroupValidator;
  }

  /**
   * Sets domain group validator.
   *
   * @param domainGroupValidator the domain group validator
   */
  @Autowired(required = false)
  public void setDomainGroupValidator(
      DomainGroupValidator domainGroupValidator) {
    if (domainGroupValidator != null) {
      this.domainGroupValidator = domainGroupValidator;
    }
  }

  @Override
  public Page<DomainGroup> getGroups(Pageable pageable, String query, Dn ou, SearchScope scope) {
    return new PageBuilder<DomainGroup, DomainGroup>()
        .sourceEntries(domainGroupRepository.findAll(query, ou, scope))
        .pageable(applyDefaults(pageable, null, true, null))
        .build();
  }


  @Override
  public DomainGroupMembers findPossibleMembers(String samAccountName, Dn ou,
      SearchScope searchScope) {
    return DomainGroupMembers.from(domainGroupRepository
        .findPossibleMembers(samAccountName, ou, searchScope));
  }

  @Override
  public Stream<DomainGroup> getMembership(String samAccountName, Dn ou,
      SearchScope searchScope) {
    return domainGroupRepository.getMembership(samAccountName, ou, searchScope);
  }

  @Override
  public DomainGroupMembers queryPossibleMembers(String samAccountName, Dn ou,
      SearchScope searchScope, String query) {
    return DomainGroupMembers.from(domainGroupRepository
        .queryPossibleMembers(samAccountName, ou, searchScope, query));
  }


  @Override
  public DomainGroup addGroup(DomainGroup domainGroup, Dn dn) {
    //domainGroupValidator.doAddValidation(domainGroup);
    return domainGroupRepository.add(domainGroup, dn);
  }

  @Override
  public Optional<DomainGroup> getGroup(String groupName, Dn ou, SearchScope scope) {
    return domainGroupRepository.findOne(groupName, ou, scope);
  }

  @Override
  public Optional<DomainGroup> updateGroup(String groupName, DomainGroup domainGroup) {
    domainGroupValidator.doUpdateValidation(groupName, domainGroup);
    return Optional.of(domainGroupRepository.update(domainGroup));
  }

  @Override
  public DomainGroup updateGroup(String groupName, DomainGroup domainGroup, Dn newOu) {
    log.debug("updateGroup({}, {}, {})", groupName, domainGroup.getSamAccountName(), newOu);
    return domainGroupRepository.update(groupName, domainGroup, newOu);
  }

  @Override
  public Boolean deleteGroup(String groupName) {
    return domainGroupRepository.delete(groupName);
  }
}
