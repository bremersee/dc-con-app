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

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.bremersee.data.ldaptive.LdaptiveEntryMapper;
import org.bremersee.data.ldaptive.LdaptiveTemplate;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.AvatarDefault;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.repository.cli.CommandExecutor;
import org.bremersee.dccon.repository.cli.CommandExecutorResponse;
import org.bremersee.dccon.repository.cli.CommandExecutorResponseValidator;
import org.bremersee.dccon.repository.ldap.DomainUserLdapMapper;
import org.bremersee.exception.ServiceException;
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchRequest;
import org.ldaptive.io.ByteArrayValueTranscoder;
import org.ldaptive.io.StringValueTranscoder;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

/**
 * The domain user repository.
 *
 * @author Christian Bremer
 */
@Profile("ldap")
@Component("domainUserRepository")
@Slf4j
public class DomainUserRepositoryImpl extends AbstractRepository implements DomainUserRepository {

  private static final StringValueTranscoder STRING_VALUE_TRANSCODER = new StringValueTranscoder();

  private static ByteArrayValueTranscoder BYTE_ARRAY_VALUE_TRANSCODER
      = new ByteArrayValueTranscoder();

  private LdaptiveEntryMapper<DomainUser> domainUserLdapMapper;

  /**
   * Instantiates a new domain user repository.
   *
   * @param properties   the properties
   * @param ldapTemplate the ldap template
   */
  public DomainUserRepositoryImpl(
      DomainControllerProperties properties,
      LdaptiveTemplate ldapTemplate) {
    super(properties, ldapTemplate);
    domainUserLdapMapper = new DomainUserLdapMapper(properties);
  }

  /**
   * Sets domain user ldap mapper.
   *
   * @param domainUserLdapMapper the domain user ldap mapper
   */
  @SuppressWarnings("unused")
  public void setDomainUserLdapMapper(
      LdaptiveEntryMapper<DomainUser> domainUserLdapMapper) {
    if (domainUserLdapMapper != null) {
      this.domainUserLdapMapper = domainUserLdapMapper;
    }
  }

  @Override
  public Stream<DomainUser> findAll(String query) {
    final SearchRequest searchRequest = new SearchRequest(
        getProperties().getUserBaseDn(),
        new SearchFilter(getProperties().getUserFindAllFilter()));
    searchRequest.setSearchScope(getProperties().getUserFindAllSearchScope());
    searchRequest.setBinaryAttributes(DomainUser.LDAP_ATTR_AVATAR);
    if (query == null || query.trim().length() == 0) {
      return getLdapTemplate().findAll(searchRequest, domainUserLdapMapper);
    } else {
      return getLdapTemplate().findAll(searchRequest, domainUserLdapMapper)
          .filter(domainUser -> this.isQueryResult(domainUser, query.trim().toLowerCase()));
    }
  }

  private boolean isQueryResult(DomainUser domainUser, String query) {
    return query != null && query.length() > 2 && domainUser != null
        && (contains(domainUser.getDisplayName(), query)
        || contains(domainUser.getUserName(), query)
        || contains(domainUser.getEmail(), query)
        || contains(domainUser.getMobile(), query)
        || contains(domainUser.getTelephoneNumber(), query)
        || contains(domainUser.getDescription(), query)
        || contains(domainUser.getFirstName(), query)
        || contains(domainUser.getLastName(), query)
        || contains(domainUser.getGroups(), query));
  }

  @Override
  public Optional<DomainUser> findOne(String userName, Boolean addAvailableGroups) {
    final SearchFilter searchFilter = new SearchFilter(getProperties().getUserFindOneFilter());
    searchFilter.setParameter(0, userName);
    final SearchRequest searchRequest = new SearchRequest(
        getProperties().getUserBaseDn(),
        searchFilter);
    searchRequest.setSearchScope(getProperties().getUserFindOneSearchScope());
    searchRequest.setBinaryAttributes(DomainUser.LDAP_ATTR_AVATAR);
    searchRequest.setSizeLimit(1L);
    return getLdapTemplate().findOne(searchRequest, domainUserLdapMapper)
        .map(domainUser -> Boolean.TRUE.equals(addAvailableGroups)
            ? addAvailableGroups(domainUser)
            : domainUser);
  }

  @Override
  public Optional<byte[]> findAvatar(String userName, AvatarDefault avatarDefault, Integer size) {
    final SearchFilter searchFilter = new SearchFilter(getProperties().getUserFindOneFilter());
    searchFilter.setParameter(0, userName);
    final SearchRequest searchRequest = new SearchRequest(
        getProperties().getUserBaseDn(),
        searchFilter);
    searchRequest.setSearchScope(getProperties().getUserFindOneSearchScope());
    searchRequest.setBinaryAttributes(DomainUser.LDAP_ATTR_AVATAR);
    searchRequest.setReturnAttributes(DomainUser.LDAP_ATTR_AVATAR);
    searchRequest.setReturnAttributes("mail");
    searchRequest.setSizeLimit(1L);

    return getLdapTemplate().findOne(searchRequest)
        .map(ldapEntry -> {
          byte[] avatar = LdaptiveEntryMapper.getAttributeValue(
              ldapEntry, DomainUser.LDAP_ATTR_AVATAR, BYTE_ARRAY_VALUE_TRANSCODER, null);
          if (avatar != null && avatar.length > 0) {
            // TODO size
            return avatar;
          }
          String mail = LdaptiveEntryMapper
              .getAttributeValue(ldapEntry, "mail", STRING_VALUE_TRANSCODER, null);
          if (StringUtils.hasText(mail)) {
            final byte[] md5 = DigestUtils.md5Digest(mail.getBytes(StandardCharsets.UTF_8));
            final String hex = new String(Hex.encode(md5));
            final String defaultAvatar = avatarDefault != null
                ? avatarDefault.toString()
                : AvatarDefault.NOT_FOUND.toString();
            final String avatarSize =
                size == null || size < 1 || size > 2048 ? "80" : size.toString();
            final String url = getProperties().getGravatarUrl()
                .replace("{hash}", hex)
                .replace("{default}", defaultAvatar)
                .replace("{size}", avatarSize);
            try {
              return IOUtils.toByteArray(new URL(url));
            } catch (Exception e) {
              if (AvatarDefault.NOT_FOUND.toString().equalsIgnoreCase(defaultAvatar)) {
                return null;
              }
              log.error("msg=[Getting avatar failed. This should not happen.] url=[{}]",
                  url, e);
            }
          }
          // TODO return image for entries with no email
          return null;
        });
  }

  @Override
  public boolean exists(String userName) {
    return getLdapTemplate()
        .exists(DomainUser.builder().userName(userName).build(), domainUserLdapMapper);
  }

  @Override
  public DomainUser save(DomainUser domainUser) {
    if (!exists(domainUser.getUserName())) {
      kinit();
      final List<String> commands = new ArrayList<>();
      sudo(commands);
      commands.add(getProperties().getSambaToolBinary());
      commands.add("user");
      commands.add("create");
      commands.add(domainUser.getUserName());
      if (StringUtils.hasText(domainUser.getPassword())) {
        commands.add("'" + domainUser.getPassword() + "'");
      } else {
        commands.add("--random-password");
      }
      commands.add("--use-username-as-cn");
      auth(commands);

      CommandExecutor.exec(
          commands,
          null,
          getProperties().getSambaToolExecDir(),
          (CommandExecutorResponseValidator) response -> {
            final String err = response.stderrToOneLine();
            if (err.startsWith("ERROR:") && err.contains("check_password_restrictions")) {
              throw ServiceException.badRequest(
                  "msg=[The password does not meet the complexity criteria!] userName=["
                      + domainUser.getUserName() + "]",
                  "check_password_restrictions");
            }
            if (!exists(domainUser.getUserName())) {
              throw ServiceException.internalServerError("msg=[Saving user failed.] userName=["
                      + domainUser.getUserName() + "] "
                      + CommandExecutorResponse.toExceptionMessage(response),
                  "org.bremersee:dc-con-app:216e1246-b464-48f1-ac88-20e8461dea1e");
            }
          });
    }
    return getLdapTemplate().save(domainUser, domainUserLdapMapper);
  }

  @Override
  public void savePassword(String userName, String newPassword) {
    kinit();
    final List<String> commands = new ArrayList<>();
    sudo(commands);
    commands.add(getProperties().getSambaToolBinary());
    commands.add("user");
    commands.add("setpassword");
    commands.add(userName);
    commands.add("--newpassword='" + newPassword + "'"); // works
    auth(commands);
    CommandExecutor.exec(
        commands,
        null,
        getProperties().getSambaToolExecDir(),
        (CommandExecutorResponseValidator) response -> {
          final String err = response.stderrToOneLine();
          if (err.startsWith("ERROR:") && err.contains("check_password_restrictions")) {
            throw ServiceException.badRequest(
                "msg=[The password does not meet the complexity criteria!] userName=["
                    + userName + "]",
                "check_password_restrictions");
          } else if (StringUtils.hasText(err)) {
            throw ServiceException.internalServerError(
                "msg=[Setting new password failed.] userName=[" + userName + "] "
                    + CommandExecutorResponse.toExceptionMessage(response),
                "org.bremersee:dc-con-app:abc34c93-920e-4600-b5b4-3ce215a9fdeb");
          }
        });
  }

  @Override
  public boolean delete(String userName) {

    if (exists(userName)) {
      kinit();
      final List<String> commands = new ArrayList<>();
      sudo(commands);
      commands.add(getProperties().getSambaToolBinary());
      commands.add("user");
      commands.add("delete");
      commands.add(userName);
      auth(commands);
      CommandExecutor.exec(
          commands,
          null,
          getProperties().getSambaToolExecDir(),
          (CommandExecutorResponseValidator) response -> {
            if (exists(userName)) {
              throw ServiceException.internalServerError(
                  "msg=[Deleting user failed.] userName=[" + userName + "] "
                      + CommandExecutorResponse.toExceptionMessage(response));
            }
          });
      return true;
    }
    return false;
  }
}
