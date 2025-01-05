/*
 * Copyright 2025 the original author or authors.
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

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.AvatarDefault;
import org.bremersee.ldaptive.LdaptiveTemplate;
import org.ldaptive.dn.Dn;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * The type AbstractDomainUserRepository.
 *
 * @author Christian Bremer
 */
@Slf4j
abstract class AbstractDomainUserRepository extends AbstractRepository
    implements DomainUserRepository {

  @Getter(AccessLevel.PACKAGE)
  private final DomainRepository domainRepository;

  @Getter(AccessLevel.PACKAGE)
  private final List<AvatarProvider> avatarProviders;

  @Getter(AccessLevel.PACKAGE)
  private Pattern passwordPattern;

  AbstractDomainUserRepository(
      DomainControllerProperties properties,
      LdaptiveTemplate ldapTemplate,
      DomainRepository domainRepository,
      List<AvatarProvider> avatarProviders) {
    super(properties, ldapTemplate);
    this.domainRepository = domainRepository;
    this.avatarProviders = avatarProviders;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void init() {
    passwordPattern = domainRepository.getPasswordInformation().getPasswordPattern();
  }

  @Override
  Dn getDefaultOu() {
    return getProperties().getUser().getDefaultUserOu();
  }

  @Override
  String getObjectClassValue() {
    return LDAP_OBJECT_CLASS_USER;
  }

  @Override
  String[] getBinaryAttributes() {
    return LDAP_USER_BINARY_ATTRIBUTES;
  }

  @Override
  String[] getReturnAttributes() {
    return LDAP_USER_MAPPED_ATTRIBUTES;
  }

  Stream<byte[]> findAvatarsOfProviders(String user, AvatarDefault avatarDefault,
      Integer size) {
    log.debug("findAvatarsOfProviders({}, {}, {})", user, avatarDefault, size);
    return getAvatarProviders().stream()
        .flatMap(repo -> repo.findAvatar(user, avatarDefault, size).stream())
        .filter(this::isNotEmpty);
  }

}
