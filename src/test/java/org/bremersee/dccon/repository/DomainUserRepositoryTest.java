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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.bremersee.data.ldaptive.LdaptiveTemplate;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.AvatarDefault;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.repository.ldap.DomainUserLdapConstants;
import org.bremersee.dccon.repository.ldap.DomainUserLdapMapper;
import org.bremersee.exception.ServiceException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

/**
 * The domain user repository test.
 */
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
   */
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
        groupRepository);
    userRepository.setDomainUserLdapMapper(new DomainUserLdapMapper(properties));
    userRepository = spy(userRepository);
    doNothing().when(userRepository).doAdd(any());
    doNothing().when(userRepository).doDelete(anyString());
  }

  /**
   * Reset ldaptive template.
   */
  @BeforeEach
  void resetLdaptiveTemplate() {
    reset(ldaptiveTemplate);
  }

  /**
   * Find all.
   */
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
    assertTrue(userRepository.findAll(null)
        .anyMatch(user -> user0.getUserName().equals(user.getUserName())));
    assertTrue(userRepository.findAll(null)
        .anyMatch(user -> user1.getUserName().equals(user.getUserName())));
  }

  /**
   * Find all with query.
   */
  @Test
  void findAllWithQuery() {
    DomainUser user0 = DomainUser.builder()
        .userName("user0")
        .build();
    DomainUser user1 = DomainUser.builder()
        .userName("user1")
        .groups(Collections.singletonList("group1"))
        .build();
    when(ldaptiveTemplate.findAll(any(), any()))
        .thenAnswer((Answer<Stream<DomainUser>>) invocationOnMock -> Stream.of(user0, user1));
    assertFalse(userRepository.findAll("group1")
        .anyMatch(user -> user0.getUserName().equals(user.getUserName())));
    assertTrue(userRepository.findAll("group1")
        .anyMatch(user -> user1.getUserName().equals(user.getUserName())));
  }

  /**
   * Find one.
   */
  @Test
  void findOne() {
    DomainUser expected = DomainUser.builder()
        .userName("user0")
        .build();
    when(ldaptiveTemplate.findOne(any(), any())).thenReturn(Optional.of(expected));
    Optional<DomainUser> actual = userRepository.findOne(expected.getUserName());
    assertNotNull(actual);
    assertTrue(actual.isPresent());
    assertEquals(expected.getUserName(), actual.get().getUserName());
  }

  /**
   * Find avatar and expect not found.
   */
  @Test
  void findAvatarAndExpectNotFound() {
    when(ldaptiveTemplate.findOne(any())).thenReturn(Optional.of(new LdapEntry()));
    assertFalse(userRepository.findAvatar("somebody", AvatarDefault.NOT_FOUND, 20).isPresent());
  }

  /**
   * Find avatar and expect no email avatar.
   */
  @Test
  void findAvatarAndExpectNoEmailAvatar() {
    when(ldaptiveTemplate.findOne(any())).thenReturn(Optional.of(new LdapEntry()));
    Optional<byte[]> actual = userRepository.findAvatar("somebody", AvatarDefault.ROBOHASH, 20);
    assertTrue(actual.isPresent());
  }

  /**
   * Find avatar from gravatar.
   */
  @Test
  void findAvatarFromGravatar() {
    LdapEntry ldapEntry = new LdapEntry();
    ldapEntry.addAttribute(new LdapAttribute("mail", "someone@example.org"));
    when(ldaptiveTemplate.findOne(any())).thenReturn(Optional.of(ldapEntry));
    Optional<byte[]> actual = userRepository.findAvatar("somebody", AvatarDefault.ROBOHASH, 20);
    assertTrue(actual.isPresent());
  }

  /**
   * Find avatar.
   *
   * @throws IOException the io exception
   */
  @Test
  void findAvatar() throws IOException {
    String location = "classpath:mp.jpg";
    ResourceLoader resourceLoader = new DefaultResourceLoader();
    byte[] expected = IOUtils.toByteArray(resourceLoader.getResource(location).getInputStream());
    LdapEntry ldapEntry = new LdapEntry();
    ldapEntry.addAttribute(new LdapAttribute(
        DomainUserLdapConstants.JPEG_PHOTO,
        expected));
    when(ldaptiveTemplate.findOne(any())).thenReturn(Optional.of(ldapEntry));
    Optional<byte[]> actual = userRepository.findAvatar("somebody", AvatarDefault.ROBOHASH, 20);
    assertTrue(actual.isPresent());
  }

  /**
   * Exists.
   */
  @Test
  void exists() {
    when(ldaptiveTemplate.exists(any(), any())).thenReturn(true);
    assertTrue(userRepository.exists("someone"));
  }

  /**
   * Save and expect service exception.
   */
  @Test
  void saveAndExpectServiceException() {
    DomainUser expected = DomainUser.builder()
        .userName("someone")
        .password("short")
        .build();
    when(ldaptiveTemplate.exists(any(), any())).thenReturn(false);
    assertThrows(
        ServiceException.class,
        () -> userRepository.save(expected, true));
  }

  /**
   * Save.
   */
  @Test
  void save() {
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
    when(groupRepository.save(any())).thenReturn(group);
    DomainUser actual = userRepository.save(expected, true);
    assertNotNull(actual);
    assertEquals(expected.getUserName(), actual.getUserName());
  }

  /**
   * Delete and expect true.
   */
  @Test
  void deleteAndExpectTrue() {
    when(ldaptiveTemplate.exists(any(), any())).thenReturn(true);
    assertTrue(userRepository.delete("someone"));
    verify(userRepository).doDelete(anyString());
  }

  /**
   * Delete and expect false.
   */
  @Test
  void deleteAndExpectFalse() {
    when(ldaptiveTemplate.exists(any(), any())).thenReturn(false);
    assertFalse(userRepository.delete("someone"));
    verify(userRepository, never()).doDelete(anyString());
  }
}