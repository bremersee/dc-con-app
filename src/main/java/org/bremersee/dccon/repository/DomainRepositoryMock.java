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

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.CommonAttributes;
import org.bremersee.dccon.model.PasswordComplexity;
import org.bremersee.dccon.model.PasswordInformation;
import org.bremersee.dccon.model.SamAccount;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * The domain repository mock.
 *
 * @author Christian Bremer
 */
@Profile("mock")
@Component("domainRepositoryMock")
@Slf4j
public class DomainRepositoryMock extends AbstractDomainRepository
    implements DomainRepository, RepositoryMock {

  private final RepositoryMockStore store;

  public DomainRepositoryMock(
      DomainControllerProperties properties,
      RepositoryMockStore store) {
    super(properties, null);
    this.store = store;
  }

  @Override
  public void resetData() {
    // noting to do
  }

  @Override
  public boolean dnExistsWithAnyObjectClass(String dn, String... objectClasses) {
    return store.findCommonAttributes()
        .map(CommonAttributes::getDistinguishedName)
        .anyMatch(n -> n.equalsIgnoreCase(dn));
  }

  @Override
  public Optional<String> findDnOfSamAccountName(String samAccountName) {
    return store.findCommonAttributes()
        .filter(e -> e instanceof SamAccount)
        .filter(e -> ((SamAccount) e).getSamAccountName()
            .equalsIgnoreCase(samAccountName))
        .map(CommonAttributes::getDistinguishedName)
        .findFirst();
  }

  @Override
  public boolean isRfc2307Enabled() {
    return true;
  }

  @Override
  public PasswordInformation getPasswordInformation() {
    return PasswordInformation.builder()
        .minimumPasswordLength(8)
        .passwordComplexity(PasswordComplexity.ON)
        .build();
  }

}
