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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bremersee.ldaptive.LdaptiveTemplate;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * The domain group repository implementation test.
 *
 * @author Christian Bremer
 */
@Disabled
class DomainGroupRepositoryImplTest {

  private static LdaptiveTemplate ldaptiveTemplate;

  private static DomainGroupRepositoryImpl groupRepository;

  private static ObjectProvider<LdaptiveTemplate> ldapTemplateProvider(LdaptiveTemplate template) {
    //noinspection unchecked
    ObjectProvider<LdaptiveTemplate> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(template);
    return provider;
  }

  /**
   * Init.
   *
  @BeforeAll
  static void init() {
    DomainControllerProperties properties = new DomainControllerProperties();
    properties.setGroupBaseDn("ou=group");
    properties.setUserBaseDn("ou=users");

    ldaptiveTemplate = mock(LdaptiveTemplate.class);

    groupRepository = new DomainGroupRepositoryImpl(
        properties,
        ldapTemplateProvider(ldaptiveTemplate));
    groupRepository.setDomainGroupLdapMapper(new DomainGroupLdapMapper(properties));
    groupRepository = spy(groupRepository);
    doNothing().when(groupRepository).doAdd(any());
    doNothing().when(groupRepository).doDelete(anyString());
  }

  /**
   * Reset ldaptive template.
   *
  @BeforeEach
  void resetLdaptiveTemplate() {
    reset(ldaptiveTemplate);
  }

  /**
   * Find all.
   *
  @Test
  void findAll() {
    DomainGroup group0 = DomainGroup.builder()
        .name("group0")
        .build();
    DomainGroup group1 = DomainGroup.builder()
        .name("group1")
        .build();
    when(ldaptiveTemplate.findAll(any(), any()))
        .thenAnswer((Answer<Stream<DomainGroup>>) invocationOnMock -> Stream.of(group0, group1));
    assertThat(groupRepository.findAll(null))
        .map(DomainGroup::getSamAccountName)
        .containsExactlyInAnyOrder(group0.getSamAccountName(), group1.getSamAccountName());
  }

  /**
   * Find all with query.
   *
  @Test
  void findAllWithQuery() {
    DomainGroup group0 = DomainGroup.builder()
        .name("group0")
        .build();
    DomainGroup group1 = DomainGroup.builder()
        .name("group1")
        .description("My second group")
        .build();
    when(ldaptiveTemplate.findAll(any(), any()))
        .thenAnswer((Answer<Stream<DomainGroup>>) invocationOnMock -> Stream.of(group1));
    assertThat(groupRepository.findAll("second"))
        .map(DomainGroup::getSamAccountName)
        .contains(group1.getSamAccountName())
        .doesNotContain(group0.getSamAccountName());
  }

  /**
   * Find one.
   *
  @Test
  void findOne() {
    DomainGroup expected = DomainGroup.builder()
        .name("group0")
        .build();
    when(ldaptiveTemplate.findOne(any(), any())).thenReturn(Optional.of(expected));
    Optional<DomainGroup> actual = groupRepository.findOne(expected.getSamAccountName());
    assertThat(actual)
        .isPresent()
        .map(DomainGroup::getSamAccountName)
        .hasValue(expected.getSamAccountName());
  }

  /**
   * Exists.
   *
  @Test
  void exists() {
    when(ldaptiveTemplate.exists(any(), any())).thenReturn(true);
    assertThat(groupRepository.exists("name"))
        .isTrue();
  }

  /**
   * Save.
   *
  @Test
  void add() {
    when(ldaptiveTemplate.exists(any(), any())).thenReturn(false);
    DomainGroup expected = DomainGroup.builder()
        .name("group0")
        .build();
    when(ldaptiveTemplate.save(any(), any())).thenReturn(expected);
    DomainGroup actual = groupRepository.add(expected);
    verify(groupRepository).doAdd(any());
    assertThat(actual)
        .extracting(DomainGroup::getSamAccountName)
        .isEqualTo(expected.getSamAccountName());
  }

  /**
   * Delete and expect true.
   */
  @Test
  void deleteAndExpectTrue() {
    when(ldaptiveTemplate.exists(any(), any())).thenReturn(true);
    assertThat(groupRepository.delete("group0"))
        .isTrue();
    verify(groupRepository).doDelete(anyString());
  }

  /**
   * Delete and expect false.
   */
  @Test
  void deleteAndExpectFalse() {
    when(ldaptiveTemplate.exists(any(), any())).thenReturn(false);
    assertThat(groupRepository.delete("group0"))
        .isFalse();
    verify(groupRepository, never()).doDelete(anyString());
  }
}