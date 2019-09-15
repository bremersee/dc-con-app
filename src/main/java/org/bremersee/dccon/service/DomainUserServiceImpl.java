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
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.model.Password;
import org.bremersee.dccon.repository.DomainUserRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * The domain user service.
 *
 * @author Christian Bremer
 */
@Component("domainUserService")
@Slf4j
public class DomainUserServiceImpl implements DomainUserService {

  private final DomainUserRepository domainUserRepository;

  /**
   * Instantiates a new domain user service.
   *
   * @param domainUserRepository the domain user repository
   */
  public DomainUserServiceImpl(
      final DomainUserRepository domainUserRepository) {
    this.domainUserRepository = domainUserRepository;
  }

  @Override
  public List<DomainUser> getUsers(final String sort) {
    final String sortOrder = StringUtils.hasText(sort) ? sort : DomainUser.DEFAULT_SORT_ORDER;
    return domainUserRepository.findAll()
        .sorted(ComparatorBuilder.builder()
            .fromWellKnownText(sortOrder)
            .build())
        .collect(Collectors.toList());
  }

  @Override
  public DomainUser addUser(@NotNull @Valid DomainUser domainUser) {
    // TODO validate home etc.
    return domainUserRepository.save(domainUser);
  }

  @Override
  public Optional<DomainUser> getUser(@NotNull String userName) {
    return domainUserRepository.findOne(userName);
  }

  @Override
  public Optional<DomainUser> updateUser(
      @NotNull String userName,
      Boolean updateGroups,
      @NotNull @Valid DomainUser domainUser) {

    return domainUserRepository.findOne(userName)
        .map(oldDomainUser -> {
          if (!Boolean.TRUE.equals(updateGroups)) {
            domainUser.setGroups(oldDomainUser.getGroups());
          }
          domainUser.setUserName(userName);
          // TODO validate home etc.
          return domainUserRepository.save(domainUser);
        });
  }

  @Override
  public void updateUserPassword(@NotNull String userName, @NotNull @Valid Password newPassword) {
    domainUserRepository.savePassword(userName, newPassword.getValue());
  }

  @Override
  public Boolean userExists(@NotNull String userName) {
    return domainUserRepository.exists(userName);
  }

  @Override
  public Boolean deleteUser(@NotNull String userName) {
    return domainUserRepository.delete(userName);
  }

}
