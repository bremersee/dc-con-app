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

import static org.bremersee.dccon.repository.RepositoryMockStore.updateCommonAttributes;

import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.model.DomainGroupMember;
import org.bremersee.dccon.model.TreeSearchScope;
import org.bremersee.exception.ServiceException;
import org.ldaptive.dn.Dn;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * The domain group repository mock.
 *
 * @author Christian Bremer
 */
@Profile("mock")
@Component("domainGroupRepositoryMock")
@Slf4j
public class DomainGroupRepositoryMock extends AbstractDomainGroupRepository
    implements DomainGroupRepository, RepositoryMock {

  private final RepositoryMockStore store;

  public DomainGroupRepositoryMock(
      DomainControllerProperties properties,
      RepositoryMockStore store,
      DomainRepository domainRepository) {
    super(properties, null, domainRepository);
    this.store = store;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void init() {
    resetData();
  }

  @Override
  public void resetData() {
    // TODO
  }

  @Override
  public Stream<DomainGroup> findAll(String query, Dn ou, TreeSearchScope searchScope) {
    final boolean all = query == null || query.length() <= 2;
    return store.getGroupRepo().values().stream()
        .filter(domainGroup -> all
            || isQueryResult(domainGroup, query.toLowerCase()));
  }

  @Override
  public Stream<DomainGroup> resolveMemberships(String samAccountName, Dn ou,
      TreeSearchScope searchScope) {
    return Stream.empty();
  }

  @Override
  public Stream<DomainGroupMember> findPossibleMembers(String groupName, Dn ou,
      TreeSearchScope searchScope) {
    return Stream.empty();
  }

  @Override
  public Stream<DomainGroup> getMemberships(String samAccountName, Dn ou, TreeSearchScope searchScope) {
    return Stream.empty();
  }

  @Override
  public Stream<DomainGroupMember> queryPossibleMembers(String groupName, Dn ou,
      TreeSearchScope searchScope, String query) {
    return Stream.empty();
  }

  @Override
  public Stream<DomainGroupMember> getMembers(String groupName, Dn ou, TreeSearchScope searchScope) {
    return Stream.empty();
  }

  private boolean isQueryResult(final DomainGroup domainGroup, final String query) {
    return query != null && domainGroup != null
        && (contains(domainGroup.getSamAccountName(), query)
        || contains(domainGroup.getDescription(), query)
        || contains(domainGroup.getMembers(), query));
  }

  @Override
  public Optional<DomainGroup> findOne(String groupName, Dn ou, TreeSearchScope searchScope) {
    return Optional.ofNullable(store.getGroupRepo().get(groupName.toLowerCase()));
  }

  @Override
  public DomainGroup add(DomainGroup domainGroup, Dn ou) {
    if (store.getGroupRepo().size() > MAX_ENTRIES) {
      throw ServiceException.internalServerError(
          "Maximum size of groups is exceeded.",
          EC_MAX_MOCK_DATA);
    }
    updateCommonAttributes(domainGroup, getProperties().getBaseDn(validateOu(ou)), "CN",
        domainGroup.getSamAccountName());
    return update(domainGroup);
  }

  @Override
  public DomainGroup update(DomainGroup domainGroup) {
    store.getGroupRepo().put(domainGroup.getSamAccountName().toLowerCase(), domainGroup);
    return domainGroup;
  }

  public DomainGroup update(String groupName, DomainGroup domainGroup, Dn newOu) {
    // TODO
    return null;
  }

  @Override
  public boolean delete(String groupName) {
    return store.getGroupRepo().remove(groupName.toLowerCase()) != null;
  }

}
