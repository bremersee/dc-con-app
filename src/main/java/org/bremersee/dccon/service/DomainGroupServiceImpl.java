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

package org.bremersee.dccon.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.comparator.ComparatorBuilder;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.repository.DomainGroupRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author Christian Bremer
 */
@Component("domainGroupService")
@Slf4j
public class DomainGroupServiceImpl implements DomainGroupService {

  private DomainGroupRepository domainGroupRepository;

  public DomainGroupServiceImpl(
      DomainGroupRepository domainGroupRepository) {
    this.domainGroupRepository = domainGroupRepository;
  }

  @Override
  public List<DomainGroup> getGroups(String sort) {
    final String sortOrder = StringUtils.hasText(sort) ? sort : DomainGroup.DEFAULT_SORT_ORDER;
    return domainGroupRepository.findAll()
        .sorted(ComparatorBuilder.builder()
            .fromWellKnownText(sortOrder)
            .build())
        .collect(Collectors.toList());
  }

  @Override
  public DomainGroup addGroup(@NotNull @Valid DomainGroup domainGroup) {
    return domainGroupRepository.save(domainGroup);
  }

  @Override
  public Optional<DomainGroup> getGroupByName(@NotNull String groupName) {
    return domainGroupRepository.findOne(groupName);
  }

  @Override
  public Optional<DomainGroup> updateGroup(@NotNull String groupName,
      @NotNull @Valid DomainGroup domainGroup) {
    return domainGroupRepository.findOne(groupName)
        .map(oldDomainGroup -> {
          domainGroup.setName(groupName);
          return domainGroupRepository.save(domainGroup);
        });
  }

  @Override
  public Boolean groupExists(@NotNull String groupName) {
    return domainGroupRepository.exists(groupName);
  }

  @Override
  public Boolean deleteGroup(@NotNull String groupName) {
    return domainGroupRepository.delete(groupName);
  }
}
