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

package org.bremersee.dccon.repository;

import static org.bremersee.dccon.config.DomainControllerProperties.getComplexPasswordRegex;
import static org.bremersee.dccon.repository.DomainUserRepositoryImpl.isQueryResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.AvatarDefault;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.model.PasswordInformation;
import org.bremersee.exception.ServiceException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * The domain user repository mock.
 *
 * @author Christian Bremer
 */
@Profile("!ldap")
@Component
@Slf4j
public class DomainUserRepositoryMock implements DomainUserRepository, MockRepository {

  private static final String USERS_LOCATION = "classpath:demo/users.json";

  private static final String GROUPS_LOCATION = "classpath:demo/groups.json";

  private final ResourceLoader resourceLoader = new DefaultResourceLoader();

  private final Map<String, DomainUser> repo = new ConcurrentHashMap<>();

  private Pattern passwordPattern = Pattern.compile(getComplexPasswordRegex(7));

  private ObjectMapper objectMapper;

  private DomainRepository domainRepository;

  private DomainGroupRepository groupRepository;

  public DomainUserRepositoryMock(
      Jackson2ObjectMapperBuilder objectMapperBuilder,
      DomainRepository domainRepository,
      DomainGroupRepository groupRepository) {
    this.objectMapper = objectMapperBuilder.build();
    this.domainRepository = domainRepository;
    this.groupRepository = groupRepository;
  }

  /**
   * Init.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void init() {
    log.warn("\n"
        + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n"
        + "!! MOCK is running:  DomainUserRepository                                           !!\n"
        + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
    );
    final PasswordInformation info = domainRepository.getPasswordInformation();
    final int minLength = info.getMinimumPasswordLength() != null
        ? info.getMinimumPasswordLength()
        : 7;
    passwordPattern = Pattern.compile(getComplexPasswordRegex(minLength));
    resetData();
  }

  @Override
  public void resetData() {
    DomainUser[] users;
    DomainGroup[] groups;
    try {
      users = objectMapper.readValue(
          resourceLoader.getResource(USERS_LOCATION).getInputStream(), DomainUser[].class);
      groups = objectMapper.readValue(
          resourceLoader.getResource(GROUPS_LOCATION).getInputStream(), DomainGroup[].class);
    } catch (IOException e) {
      throw ServiceException.internalServerError("Loading demo data failed.", e);
    }
    groupRepository.findAll(null).forEach(group -> groupRepository.delete(group.getName()));
    repo.clear();
    for (DomainUser user : users) {
      if (!StringUtils.hasText(user.getPassword())) {
        user.setPassword(domainRepository.createRandomPassword());
      }
      repo.put(user.getUserName().toLowerCase(), user);
    }
    for (DomainGroup group : groups) {
      groupRepository.save(group);
    }
  }

  private List<String> findDomainGroups(final String userName) {
    return groupRepository.findAll(null)
        .filter(domainGroup -> domainGroup.getMembers().contains(userName))
        .map(DomainGroup::getName)
        .collect(Collectors.toList());
  }

  @Override
  public Stream<DomainUser> findAll(final String query) {
    final boolean all = query == null || query.trim().length() == 0;
    return repo.values().stream()
        .filter(domainUser -> all || isQueryResult(domainUser, query.trim().toLowerCase()))
        .map(domainUser -> domainUser.toBuilder()
            .groups(findDomainGroups(domainUser.getUserName()))
            .build());
  }

  @Override
  public Optional<DomainUser> findOne(final String userName) {
    return Optional.ofNullable(repo.get(userName.toLowerCase()))
        .flatMap(domainUser -> Optional.of(domainUser
            .toBuilder()
            .groups(findDomainGroups(domainUser.getUserName()))
            .password(null)
            .build()));
  }

  @Override
  public Optional<byte[]> findAvatar(
      final String userName,
      final AvatarDefault avatarDefault,
      final Integer size) {

    final int avatarSize = size == null || size < 1 || size > 2048 ? 80 : size;
    return findOne(userName)
        .flatMap(domainUser -> Optional.ofNullable(
            DomainUserRepositoryImpl.findAvatar(
                domainUser.getEmail(),
                new DomainControllerProperties().getGravatarUrl(),
                avatarDefault,
                avatarSize)));
  }

  @Override
  public boolean exists(final String userName) {
    return repo.get(userName.toLowerCase()) != null;
  }

  @Override
  public DomainUser save(final DomainUser domainUser, final Boolean updateGroups) {

    if (repo.size() > 100 && repo.get(domainUser.getUserName().toLowerCase()) == null) {
      throw ServiceException.internalServerError(
          "Maximum size of users is exceeded.",
          "org.bremersee:dc-con-app:9272263e-2074-46f7-ba64-23978981a1d3");
    }
    if (!StringUtils.hasText(domainUser.getPassword())) {
      domainUser.setPassword(domainRepository.createRandomPassword());
    }
    if (!passwordPattern.matcher(domainUser.getPassword()).matches()) {
      throw ServiceException.badRequest(
          "msg=[The password does not meet the complexity criteria!] userName=["
              + domainUser.getUserName() + "]",
          "check_password_restrictions");
    }
    final boolean doGroupUpdate = Boolean.TRUE.equals(updateGroups);
    final Set<String> groups = doGroupUpdate
        ? new HashSet<>(domainUser.getGroups())
        : Collections.emptySet();
    domainUser.getGroups().clear();
    repo.put(domainUser.getUserName().toLowerCase(), domainUser);

    if (doGroupUpdate) {
      groups.forEach(groupName -> groupRepository.findOne(groupName).ifPresent(domainGroup -> {
        if (!domainGroup.getMembers().contains(domainUser.getUserName())) {
          domainGroup.getMembers().add(domainUser.getUserName());
          groupRepository.save(domainGroup);
        }
      }));
    }
    return domainUser.toBuilder().groups(findDomainGroups(domainUser.getUserName())).build();
  }

  @Override
  public void savePassword(final String userName, final String newPassword) {
    final DomainUser domainUser = repo.get(userName.toLowerCase());
    if (domainUser == null) {
      throw ServiceException.notFound(DomainUser.class.getSimpleName(), userName);
    }
    if (!passwordPattern.matcher(newPassword).matches()) {
      throw ServiceException.badRequest(
          "msg=[The password does not meet the complexity criteria!] userName=["
              + domainUser.getUserName() + "]",
          "check_password_restrictions");
    }
    domainUser.setPassword(newPassword);
  }

  @Override
  public boolean delete(final String userName) {
    return Optional.ofNullable(repo.remove(userName.toLowerCase()))
        .map(DomainUser::getUserName)
        .map(name -> {
          groupRepository.findAll(null).forEach(domainGroup -> {
            if (domainGroup.getMembers().remove(name)) {
              groupRepository.save(domainGroup);
            }
          });
          return true;
        })
        .orElse(false);
  }
}
