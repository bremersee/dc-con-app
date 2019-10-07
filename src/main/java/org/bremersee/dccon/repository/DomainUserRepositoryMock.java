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

import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.model.DomainUser;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * The domain user repository mock.
 *
 * @author Christian Bremer
 */
@Profile("!ldap")
@Component
@Slf4j
public class DomainUserRepositoryMock implements DomainUserRepository {

  /**
   * Init.
   */
  @PostConstruct
  public void init() {
    log.warn("\n"
        + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n"
        + "!! MOCK is running:  DomainUserRepository                                           !!\n"
        + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
    );
  }

  @Override
  public Stream<DomainUser> findAll(String query) {
    return Stream.empty();
  }

  @Override
  public Optional<DomainUser> findOne(@NotNull String userName, Boolean addAvailableGroups) {
    return Optional.empty();
  }

  @Override
  public boolean exists(@NotNull String userName) {
    return false;
  }

  @Override
  public DomainUser save(@NotNull DomainUser domainUser) {
    return DomainUser.builder().userName(domainUser.getUserName()).build();
  }

  @Override
  public void savePassword(@NotNull String userName, @NotNull String newPassword) {

  }

  @Override
  public boolean delete(@NotNull String userName) {
    return false;
  }
}
