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

package org.bremersee.dccon.repository;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.bremersee.dccon.repository.RepositoryMockStore.updateCommonAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.AvatarDefault;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.model.TreeSearchScope;
import org.bremersee.dccon.repository.img.ImageUtils;
import org.bremersee.exception.ServiceException;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
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
@Profile("mock")
@Component("domainUserRepositoryMock")
@Slf4j
public class DomainUserRepositoryMock extends AbstractDomainUserRepository
    implements RepositoryMock {

  private static final String USERS_LOCATION = "classpath:demo/users.json";

  private static final String GROUPS_LOCATION = "classpath:demo/groups.json";

  private final ResourceLoader resourceLoader = new DefaultResourceLoader();

  private final RepositoryMockStore store;

  private final ObjectMapper objectMapper;

  /**
   * Instantiates a new Domain user repository mock.
   *
   * @param objectMapperBuilder the object mapper builder
   * @param domainRepository the domain repository
   * @param avatarProviders the avatar repositories
   */
  public DomainUserRepositoryMock(
      DomainControllerProperties properties,
      RepositoryMockStore store,
      Jackson2ObjectMapperBuilder objectMapperBuilder,
      DomainRepository domainRepository,
      List<AvatarProvider> avatarProviders) {
    super(properties, null, domainRepository, avatarProviders);
    this.store = store;
    this.objectMapper = objectMapperBuilder.build();
  }

  /**
   * Init.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void init() {
    super.init();
    resetData();
  }

  @Override
  public void resetData() {
    /*
    avatarRepo.clear();
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
    groupRepository.findAll(null).forEach(group -> groupRepository.delete(group.getSamAccountName()));
    repo.clear();
    for (DomainUser user : users) {
      if (!StringUtils.hasText(user.getPassword())) {
        user.setPassword(domainRepository.createRandomPassword());
      }
      repo.put(user.getSamAccountName().toLowerCase(), user);
    }
    for (DomainGroup group : groups) {
      groupRepository.save(group);
    }
    */
  }

  /*
  private List<String> findDomainGroups(final String userName) {
    return groupRepository.findAll(null)
        .filter(domainGroup -> domainGroup.getMembers().contains(userName))
        .map(DomainGroup::getSamAccountName)
        .sorted()
        .collect(Collectors.toList());
  }
  */

  @Override
  public Stream<DomainUser> findAll(final String query, Dn ou, TreeSearchScope scope) {
    final boolean all = query == null || query.length() <= 2;
    return store.getUserRepo().values().stream()
        .filter(domainUser -> all || isQueryResult(domainUser, query.toLowerCase()));
  }

  private boolean isQueryResult(DomainUser domainUser, String query) {
    return nonNull(query) && domainUser != null
        && (contains(domainUser.getDisplayName(), query)
        || contains(domainUser.getSamAccountName(), query)
        || contains(domainUser.getEmail(), query)
        || contains(domainUser.getMobile(), query)
        || contains(domainUser.getTelephoneNumber(), query)
        || contains(domainUser.getDescription(), query)
        || contains(domainUser.getFirstName(), query)
        || contains(domainUser.getLastName(), query));
  }

  @Override
  public Optional<DomainUser> findOne(String userName, Dn ou, TreeSearchScope searchScope) {
    return Optional.ofNullable(store.getUserRepo().get(userName.toLowerCase()));
  }

  @Override
  public boolean existsAvatarInActiveDirectory(
      String user,
      Dn ou,
      TreeSearchScope searchScope) {

    return Optional.ofNullable(user)
        .map(name -> store.getAvatarRepo().get(name.toLowerCase()))
        .map(this::isAvatarNotEmpty)
        .orElse(false);
  }

  @Override
  public Optional<byte[]> findAvatar(
      String userNameOrEmail,
      Dn ou,
      TreeSearchScope searchScope,
      AvatarDefault avatarDefault,
      Integer size) {

    return Optional.ofNullable(userNameOrEmail)
        .map(user -> store.getAvatarRepo().get(user.toLowerCase()))
        .or(() -> getAvatarProviders().stream()
            .flatMap(repo -> repo.findAvatar(userNameOrEmail, avatarDefault, size).stream())
            .findFirst());
  }

  @Override
  public void saveAvatar(final String userName, final InputStream avatar) {
    if (!exists(userName)) {
      throw ServiceException.notFoundWithErrorCode(
          DomainUser.class.getSimpleName(),
          userName,
          EC_SAM_ACCOUNT_NOT_FOUND);
    }
    try (InputStream in = avatar) {
      BufferedImage img = ImageIO.read(in);
      int width = img.getWidth();
      int height = img.getHeight();
      int max = Math.max(width, height);
      if (max > AvatarRepository.MAX_AVATAR_SIZE) {
        float factor = Integer.valueOf(AvatarRepository.MAX_AVATAR_SIZE).floatValue() / max;
        width = Math.min(Math.round(factor * width), AvatarRepository.MAX_AVATAR_SIZE);
        height = Math.min(Math.round(factor * height), AvatarRepository.MAX_AVATAR_SIZE);
        img = ImageUtils.scaleImage(img, new Dimension(width, height));
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ImageIO.write(img, "jpg", out);
      store.getAvatarRepo().put(userName.toLowerCase(), out.toByteArray());
    } catch (IOException e) {
      throw ServiceException.internalServerError("Saving avatar failed.", e);
    }
  }

  @Override
  public void removeAvatar(final String userName) {
    store.getAvatarRepo().remove(userName.toLowerCase());
  }

  private boolean exists(final String userName) {
    return nonNull(store.getUserRepo().get(userName.toLowerCase()));
  }

  @Override
  public DomainUser add(final DomainUser domainUser, Dn ou, Boolean useUsernameAsCn) {

    if (exists(domainUser.getSamAccountName())) {
      throw ServiceException.alreadyExistsWithErrorCode(
          DomainUser.class.getSimpleName(),
          domainUser.getSamAccountName(),
          EC_SAM_ACCOUNT_ALREADY_EXISTS);
    }
    if (store.getUserRepo().size() > MAX_ENTRIES) {
      throw ServiceException.internalServerError(
          "Maximum size of users is exceeded.",
          EC_MAX_MOCK_DATA);
    }
    if (!StringUtils.hasText(domainUser.getPassword())) {
      domainUser.setPassword(getDomainRepository().createRandomPassword());
    }
    if (!getPasswordPattern().matcher(domainUser.getPassword()).matches()) {
      throw ServiceException.badRequest(
          "msg=[The password does not meet the complexity criteria!] userName=["
              + domainUser.getSamAccountName() + "]",
          "check_password_restrictions");
    }
    updateCommonAttributes(domainUser, getProperties().getBaseDn(validateOu(ou)), "CN",
        domainUser.getSamAccountName());
    store.getUserRepo().put(domainUser.getSamAccountName().toLowerCase(), domainUser);
    return domainUser;
  }

  @Override
  public DomainUser update(DomainUser domainUser) {
    if (!exists(domainUser.getSamAccountName())) {
      throw ServiceException.notFoundWithErrorCode(
          DomainUser.class.getSimpleName(),
          domainUser.getSamAccountName(),
          EC_SAM_ACCOUNT_NOT_FOUND);
    }
    store.getUserRepo().put(domainUser.getSamAccountName().toLowerCase(), domainUser);
    return domainUser;
  }

  public DomainUser update(String userName, DomainUser domainUser, Dn newOu) {
    if (!exists(userName)) {
      throw ServiceException.notFoundWithErrorCode(
          DomainUser.class.getSimpleName(),
          domainUser.getSamAccountName(),
          EC_SAM_ACCOUNT_NOT_FOUND);
    }
    if (!Objects.equals(domainUser.getSamAccountName(), userName)) {
      store.getUserRepo().remove(userName);
    }
    Dn parentDn;
    if (!isNull(newOu) && !newOu.isEmpty()) {
      parentDn = domainUser.getDn().getParent();
    } else {
      parentDn = getProperties().getBaseDn(validateOu(newOu));
    }
    updateCommonAttributes(domainUser, parentDn, "CN", domainUser.getSamAccountName());
    store.getUserRepo().put(domainUser.getSamAccountName().toLowerCase(), domainUser);
    return domainUser;
  }


  @Override
  public void savePassword(final String userName, final String newPassword) {
    final DomainUser domainUser = store.getUserRepo().get(userName.toLowerCase());
    if (domainUser == null) {
      throw ServiceException.notFoundWithErrorCode(
          DomainUser.class.getSimpleName(),
          userName,
          EC_SAM_ACCOUNT_NOT_FOUND);
    }
    if (!getPasswordPattern().matcher(newPassword).matches()) {
      throw ServiceException.badRequest(
          "The password does not meet the complexity criteria!",
          "check_password_restrictions");
    }
    domainUser.setPassword(newPassword);
  }

  @Override
  public boolean delete(final String userName) {
    return Optional.ofNullable(store.getUserRepo().remove(userName.toLowerCase()))
        .map(DomainUser::getDistinguishedName)
        .map(dn -> {
          store.getGroupRepo().values()
              .forEach(group -> {
                List<String> members = new ArrayList<>(group.getMembers());
                members.remove(dn);
                group.setMembers(members);
              });
          return true;
        })
        .orElse(false);
  }

}
