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

import static org.bremersee.dccon.repository.DomainGroupRepositoryImpl.isQueryResult;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.exception.ServiceException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * The domain group repository mock.
 *
 * @author Christian Bremer
 */
@Profile("!ldap")
@Component
@Slf4j
public class DomainGroupRepositoryMock implements DomainGroupRepository {

  private final Map<String, DomainGroup> repo = new ConcurrentHashMap<>();

  /**
   * Init.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void init() {
    log.warn("\n"
        + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n"
        + "!! MOCK is running:  DomainGroupRepository                                          !!\n"
        + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
    );
  }

  @Override
  public Stream<DomainGroup> findAll(final String query) {
    final boolean all = query == null || query.trim().length() == 0;
    return repo.values().stream()
        .filter(domainGroup -> all || isQueryResult(domainGroup, query.trim().toLowerCase()))
        .map(domainGroup -> domainGroup.toBuilder().build());
  }

  @Override
  public Optional<DomainGroup> findOne(@NotNull String groupName) {
    return Optional.ofNullable(repo.get(groupName.toLowerCase()))
        .flatMap(domainGroup -> Optional.of(domainGroup.toBuilder().build()));
  }

  @Override
  public boolean exists(@NotNull String groupName) {
    return repo.get(groupName.toLowerCase()) != null;
  }

  @Override
  public DomainGroup save(@NotNull DomainGroup domainGroup) {
    if (repo.size() > 100 && repo.get(domainGroup.getName().toLowerCase()) == null) {
      throw ServiceException.internalServerError(
          "Maximum size of groups is exceeded.",
          "org.bremersee:dc-con-app:318a27fd-b083-460f-ac8e-0c59a490b391");
    }
    repo.put(domainGroup.getName().toLowerCase(), domainGroup);
    return domainGroup.toBuilder().build();
  }

  @Override
  public boolean delete(@NotNull String groupName) {
    return repo.remove(groupName.toLowerCase()) != null;
  }

}
