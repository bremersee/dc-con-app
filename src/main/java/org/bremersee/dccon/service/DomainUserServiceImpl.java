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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.comparator.ComparatorBuilder;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.model.Password;
import org.bremersee.dccon.repository.DomainGroupRepository;
import org.bremersee.dccon.repository.DomainUserRepository;
import org.bremersee.dccon.service.validator.DomainUserValidator;
import org.springframework.beans.factory.annotation.Autowired;
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

  private DomainUserValidator domainUserValidator;

  private List<AvatarService> avatarServices;

  /**
   * Instantiates a new domain user service.
   *
   * @param properties            the properties
   * @param domainUserRepository  the domain user repository
   * @param domainGroupRepository the domain group repository
   */
  public DomainUserServiceImpl(
      final DomainControllerProperties properties,
      final DomainUserRepository domainUserRepository,
      final DomainGroupRepository domainGroupRepository) {
    this.domainUserRepository = domainUserRepository;
    this.domainUserValidator = DomainUserValidator.defaultValidator(
        properties, domainGroupRepository, domainUserRepository);
    this.avatarServices = Collections.emptyList();
  }

  /**
   * Sets domain user validator.
   *
   * @param domainUserValidator the domain user validator
   */
  @Autowired(required = false)
  public void setDomainUserValidator(DomainUserValidator domainUserValidator) {
    if (domainUserValidator != null) {
      this.domainUserValidator = domainUserValidator;
    }
  }

  /**
   * Sets avatar services.
   *
   * @param avatarServices the avatar services
   */
  @Autowired(required = false)
  public void setAvatarServices(List<AvatarService> avatarServices) {
    if (avatarServices != null) {
      this.avatarServices = avatarServices;
    }
  }

  @Override
  public List<DomainUser> getUsers(final String sort, final String query) {
    final String sortOrder = StringUtils.hasText(sort) ? sort : DomainUser.DEFAULT_SORT_ORDER;
    return domainUserRepository.findAll(query)
        .sorted(ComparatorBuilder.builder()
            .fromWellKnownText(sortOrder)
            .build())
        .collect(Collectors.toList());
  }

  @Override
  public DomainUser addUser(@NotNull @Valid DomainUser domainUser) {
    domainUserValidator.doAddValidation(domainUser);
    findUserAvatar(domainUser, false);
    return domainUserRepository.save(domainUser);
  }

  @Override
  public Optional<DomainUser> getUser(@NotNull String userName) {
    return domainUserRepository.findOne(userName);
  }

  @Override
  public Optional<byte[]> getUserAvatar(
      final String userName,
      final Boolean returnDefault) {
    return domainUserRepository.findOne(userName)
        .map(domainUser -> findUserAvatar(domainUser, Boolean.TRUE.equals(returnDefault)));
  }

  private byte[] findUserAvatar(
      final DomainUser domainUser,
      final boolean returnDefault) {

    if (domainUser.getAvatar() == null) {
      for (AvatarService avatarService : avatarServices) {
        byte[] avatar = avatarService.findAvatar(domainUser, returnDefault);
        if (avatar != null) {
          domainUser.setAvatar(avatar);
          break;
        }
      }
    }
    return domainUser.getAvatar();
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
          domainUserValidator.doUpdateValidation(userName, domainUser);
          findUserAvatar(domainUser, false);
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
