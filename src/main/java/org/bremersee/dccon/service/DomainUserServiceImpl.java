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
import org.bremersee.common.model.TwoLetterLanguageCode;
import org.bremersee.comparator.ComparatorBuilder;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.AvatarDefault;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.model.Password;
import org.bremersee.dccon.repository.DomainGroupRepository;
import org.bremersee.dccon.repository.DomainUserRepository;
import org.bremersee.dccon.repository.MockRepository;
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

  private final EmailService emailService;

  private DomainUserValidator domainUserValidator;

  /**
   * Instantiates a new domain user service.
   *
   * @param properties            the properties
   * @param domainUserRepository  the domain user repository
   * @param emailService          the email service
   * @param domainGroupRepository the domain group repository
   */
  public DomainUserServiceImpl(
      final DomainControllerProperties properties,
      final DomainUserRepository domainUserRepository,
      EmailService emailService,
      final DomainGroupRepository domainGroupRepository) {
    this.domainUserRepository = domainUserRepository;
    this.emailService = emailService;
    this.domainUserValidator = DomainUserValidator.defaultValidator(
        properties, domainGroupRepository, domainUserRepository);
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

  @Override
  public void resetData() {
    if (domainUserRepository instanceof MockRepository) {
      ((MockRepository) domainUserRepository).resetData();
    } else {
      throw new UnsupportedOperationException("Reset data is not available.");
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
  public DomainUser addUser(
      final DomainUser domainUser,
      final Boolean sendEmail,
      final TwoLetterLanguageCode language) {
    domainUserValidator.doAddValidation(domainUser);
    final DomainUser addedDomainUser = domainUserRepository.save(domainUser, true);
    if (Boolean.TRUE.equals(sendEmail)) {
      emailService.sendEmailWithCredentials(
          addedDomainUser.getUserName(),
          domainUser.getPassword(),
          language);
    }
    return addedDomainUser;
  }

  @Override
  public Optional<DomainUser> getUser(@NotNull String userName) {
    return domainUserRepository.findOne(userName);
  }

  @Override
  public Optional<byte[]> getUserAvatar(
      final String userName,
      final AvatarDefault avatarDefault,
      final Integer size) {

    return domainUserRepository.findAvatar(userName, avatarDefault, size);
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
          return domainUserRepository.save(domainUser, updateGroups);
        });
  }

  @Override
  public void updateUserPassword(
      final String userName,
      final Password newPassword,
      final Boolean sendEmail,
      final TwoLetterLanguageCode language) {
    domainUserRepository.savePassword(userName, newPassword.getValue());
    if (Boolean.TRUE.equals(sendEmail)) {
      emailService.sendEmailWithCredentials(
          userName,
          newPassword.getValue(),
          language);
    }
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
