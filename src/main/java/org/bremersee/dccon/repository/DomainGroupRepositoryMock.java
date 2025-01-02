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

import static org.bremersee.dccon.repository.AbstractRepository.contains;

import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.exception.ServiceException;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * The domain group repository mock.
 *
 * @author Christian Bremer
 */
@Profile("mock")
@Component("domainGroupRepositoryMock")
@Slf4j
public class DomainGroupRepositoryMock extends AbstractRepositoryMock
    implements DomainGroupRepository {

  public DomainGroupRepositoryMock(DomainControllerProperties properties) {
    super(properties);
  }

  @Override
  public void resetData() {
    // TODO
  }

  @Override
  public Stream<DomainGroup> findAll(String query, Dn ou, SearchScope searchScope) {
    final boolean all = query == null || query.length() <= 2;
    return groupRepo.values().stream()
        .filter(domainGroup -> all
            || isQueryResult(domainGroup, query.toLowerCase()));
  }

  private boolean isQueryResult(final DomainGroup domainGroup, final String query) {
    return query != null && domainGroup != null
        && (contains(domainGroup.getSamAccountName(), query)
        || contains(domainGroup.getDescription(), query)
        || contains(domainGroup.getMembers(), query));
  }

  @Override
  public Optional<DomainGroup> findOne(String groupName, Dn ou, SearchScope searchScope) {
    return Optional.ofNullable(groupRepo.get(groupName.toLowerCase()));
  }

  public boolean exists(String groupName) {
    return groupRepo.get(groupName.toLowerCase()) != null;
  }

  @Override
  public DomainGroup add(DomainGroup domainGroup, Dn ou) {
    if (groupRepo.size() > 100) {
      throw ServiceException.internalServerError(
          "Maximum size of groups is exceeded.",
          EC_MAX_MOCK_DATA);
    }
    updateCommonAttributes(domainGroup, ou, domainGroup.getSamAccountName());
    return update(domainGroup);
  }

  @Override
  public DomainGroup update(DomainGroup domainGroup) {
    groupRepo.put(domainGroup.getSamAccountName().toLowerCase(), domainGroup);
    return domainGroup;
  }

  @Override
  public boolean delete(String groupName) {
    return groupRepo.remove(groupName.toLowerCase()) != null;
  }

}
