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

import java.io.InputStream;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.common.model.TwoLetterLanguageCode;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.AvatarDefault;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.model.Password;
import org.bremersee.dccon.repository.DomainGroupRepository;
import org.bremersee.dccon.repository.DomainUserRepository;
import org.bremersee.dccon.repository.MockRepository;
import org.bremersee.dccon.service.validator.DomainUserValidator;
import org.bremersee.pagebuilder.PageBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

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
   * @param properties the properties
   * @param domainUserRepository the domain user repository
   * @param emailService the email service
   * @param domainGroupRepository the domain group repository
   */
  public DomainUserServiceImpl(
      final DomainControllerProperties properties,
      final DomainUserRepository domainUserRepository,
      final EmailService emailService,
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
  public Page<DomainUser> getUsers(Pageable pageable, String query) {
    return new PageBuilder<DomainUser, DomainUser>()
        .sourceEntries(domainUserRepository.findAll(query))
        .pageable(applyDefaults(pageable, null, true, null))
        .build();
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
  public Optional<DomainUser> getUser(final String userName) {
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
      final String userName,
      final Boolean updateGroups,
      final DomainUser domainUser) {

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
  public void updateUserAvatar(
      final String userName,
      final InputStream avatar) {
    domainUserRepository.saveAvatar(userName, avatar);
  }

  @Override
  public void removeUserAvatar(final String userName) {
    domainUserRepository.removeAvatar(userName);
  }

  @Override
  public Boolean userExists(final String userName) {
    return domainUserRepository.exists(userName);
  }

  @Override
  public Boolean deleteUser(final String userName) {
    return domainUserRepository.delete(userName);
  }

}
