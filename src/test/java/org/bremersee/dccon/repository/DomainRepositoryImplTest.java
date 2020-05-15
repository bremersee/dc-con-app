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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.PasswordInformation;
import org.bremersee.dccon.repository.cli.PasswordInformationParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The domain repository impl test.
 *
 * @author Christian Bremer
 */
class DomainRepositoryImplTest {

  private static DomainRepositoryImpl repository;

  private static final PasswordInformation model = PasswordInformation.builder().build();

  /**
   * Init.
   */
  @BeforeAll
  static void init() {
    DomainControllerProperties properties = new DomainControllerProperties();

    repository = new DomainRepositoryImpl(properties);
    repository.setPasswordInformationParser(PasswordInformationParser.defaultParser());
    repository = spy(repository);
    doReturn(model).when(repository).getPasswordInformation();
  }

  /**
   * Gets password information.
   */
  @Test
  void getPasswordInformation() {
    assertEquals(model, repository.getPasswordInformation());
  }
}