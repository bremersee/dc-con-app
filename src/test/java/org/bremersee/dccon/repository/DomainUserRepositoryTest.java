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

import java.util.Optional;
import java.util.UUID;
import org.bremersee.dccon.model.AvatarDefault;
import org.bremersee.ldaptive.LdaptiveTemplate;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;

/**
 * The domain user repository test.
 */
@Disabled
class DomainUserRepositoryTest {

  private static LdaptiveTemplate ldaptiveTemplate;

  private static DomainGroupRepository groupRepository;

  private static DomainUserRepositoryImpl userRepository;

  private static ObjectProvider<LdaptiveTemplate> ldapTemplateProvider(LdaptiveTemplate template) {
    //noinspection unchecked
    ObjectProvider<LdaptiveTemplate> provider = mock(ObjectProvider.class);
    Mockito.when(provider.getIfAvailable()).thenReturn(template);
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

    groupRepository = mock(DomainGroupRepository.class);

    userRepository = new DomainUserRepositoryImpl(
        properties,
        ldapTemplateProvider(ldaptiveTemplate),
        new DomainRepositoryMock(),
        List.of(new GravatarRepository(properties), new FallbackAvatarRepository()));
    userRepository.setDomainUserLdapMapper(new DomainUserLdapMapper(properties));
    userRepository = spy(userRepository);
    doNothing().when(userRepository).doAdd(any());
    doNothing().when(userRepository).doDelete(anyString());
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
    DomainUser user0 = DomainUser.builder()
        .userName("user0")
        .build();
    DomainUser user1 = DomainUser.builder()
        .userName("user1")
        .build();
    when(ldaptiveTemplate.findAll(any(), any()))
        .thenAnswer((Answer<Stream<DomainUser>>) invocationOnMock -> Stream.of(user0, user1));
    assertThat(userRepository.findAll(null))
        .map(DomainUser::getSamAccountName)
        .containsExactlyInAnyOrder(user0.getSamAccountName(), user1.getSamAccountName());
  }

  /**
   * Find all with query.
   *
  @Test
  void findAllWithQuery() {
    DomainUser user0 = DomainUser.builder()
        .userName("user0")
        .build();
    DomainUser user1 = DomainUser.builder()
        .userName("user1")
        .firstName("Anna")
        .build();
    when(ldaptiveTemplate.findAll(any(), any()))
        .thenAnswer((Answer<Stream<DomainUser>>) invocationOnMock -> Stream.of(user1));
    assertThat(userRepository.findAll("anna"))
        .map(DomainUser::getSamAccountName)
        .contains(user1.getSamAccountName())
        .doesNotContain(user0.getSamAccountName());
  }

  /**
   * Find one.
   *
  @Test
  void findOne() {
    DomainUser expected = DomainUser.builder()
        .userName("user0")
        .build();
    when(ldaptiveTemplate.findOne(any(), any())).thenReturn(Optional.of(expected));
    Optional<DomainUser> actual = userRepository.findOne(expected.getSamAccountName());
    assertThat(actual)
        .isPresent()
        .map(DomainUser::getSamAccountName)
        .hasValue(expected.getSamAccountName());
  }

  /**
   * Find avatar and expect not found with ldap entry.
   */
  @Test
  void findAvatarAndExpectNotFoundWithLdapEntry() {
    String user = UUID.randomUUID().toString();
    when(ldaptiveTemplate.findOne(any())).thenReturn(Optional.of(new LdapEntry()));
    assertThat(userRepository.findAvatar(user, null, null, AvatarDefault.NOT_FOUND, 20))
        .isEmpty();
  }

  /**
   * Find avatar and expect not found without ldap entry.
   */
  @Test
  void findAvatarAndExpectNotFoundWithoutLdapEntry() {
    String user = UUID.randomUUID().toString();
    when(ldaptiveTemplate.findOne(any())).thenReturn(Optional.empty());
    assertThat(userRepository.findAvatar(user, null, null, AvatarDefault.NOT_FOUND, 20))
        .isEmpty();
  }

  /**
   * Find avatar from gravatar with valid email.
   */
  @Test
  void findAvatarFromGravatarWithValidEmail() {
    String user = "me";
    LdapEntry ldapEntry = new LdapEntry();
    ldapEntry.addAttributes(new LdapAttribute("mail", "bremersee@googlemail.com"));
    when(ldaptiveTemplate.findOne(any())).thenReturn(Optional.of(ldapEntry));
    Optional<byte[]> actual = userRepository.findAvatar(user, null, null, AvatarDefault.NOT_FOUND, 20);
    assertThat(actual)
        .isPresent();
  }

  /**
   * Find avatar from gravatar without email.
   */
  @Test
  void findAvatarFromGravatarWithoutEmail() {
    String user = UUID.randomUUID().toString();
    when(ldaptiveTemplate.findOne(any())).thenReturn(Optional.of(new LdapEntry()));
    Optional<byte[]> actual = userRepository.findAvatar(user, null, null, AvatarDefault.ROBOHASH, 20);
    assertThat(actual)
        .isPresent();
  }

  /**
   * Find avatar from gravatar without ldap entry.
   */
  @Test
  void findAvatarFromGravatarWithoutLdapEntry() {
    String user = UUID.randomUUID().toString();
    when(ldaptiveTemplate.findOne(any())).thenReturn(Optional.empty());
    Optional<byte[]> actual = userRepository.findAvatar(user, null, null, AvatarDefault.ROBOHASH, 20);
    assertThat(actual)
        .isPresent();
  }

  /**
   * Exists.
   *
  @Test
  void exists() {
    when(ldaptiveTemplate.exists(any(), any())).thenReturn(true);
    assertThat(userRepository.exists("someone"))
        .isTrue();
  }

  /**
   * Save and expect service exception.
   *
  @Test
  void saveAndExpectServiceException() {
    DomainUser expected = DomainUser.builder()
        .userName("someone")
        .password("short")
        .build();
    when(ldaptiveTemplate.exists(any(), any())).thenReturn(false);
    assertThatExceptionOfType(ServiceException.class)
        .isThrownBy(() -> userRepository.add(expected, true));
  }

  /**
   * Save.
   *
  @Test
  void add() {
    DomainUser expected = DomainUser.builder()
        .userName("someone")
        .password("this_is_A_MUCH_BETTER_on3")
        .groups(Arrays.asList("group0", "group1"))
        .build();
    DomainGroup group = DomainGroup.builder().build();
    when(ldaptiveTemplate.clone(any())).thenReturn(ldaptiveTemplate);
    when(ldaptiveTemplate.exists(any(), any())).thenReturn(false);
    when(ldaptiveTemplate.save(any(), any())).thenReturn(expected);
    when(groupRepository.findOne(anyString())).thenReturn(Optional.of(group));
    when(groupRepository.add(any())).thenReturn(group);
    DomainUser actual = userRepository.add(expected, true);
    assertThat(actual)
        .isNotNull()
        .extracting(DomainUser::getSamAccountName)
        .isEqualTo(expected.getSamAccountName());
  }

  /**
   * Delete and expect true.
   */
  @Test
  void deleteAndExpectTrue() {
    when(ldaptiveTemplate.exists(any(), any())).thenReturn(true);
    assertThat(userRepository.delete("someone"))
        .isTrue();
    verify(userRepository).doDelete(anyString());
  }

  /**
   * Delete and expect false.
   */
  @Test
  void deleteAndExpectFalse() {
    when(ldaptiveTemplate.exists(any(), any())).thenReturn(false);
    assertThat(userRepository.delete("someone"))
        .isFalse();
    verify(userRepository, never()).doDelete(anyString());
  }
}