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

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.bremersee.data.ldaptive.AbstractLdaptiveErrorHandler;
import org.bremersee.data.ldaptive.LdaptiveEntryMapper;
import org.bremersee.data.ldaptive.LdaptiveException;
import org.bremersee.data.ldaptive.LdaptiveTemplate;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.AvatarDefault;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.model.PasswordComplexity;
import org.bremersee.dccon.model.PasswordInformation;
import org.bremersee.dccon.repository.cli.CommandExecutor;
import org.bremersee.dccon.repository.cli.CommandExecutorResponse;
import org.bremersee.dccon.repository.cli.CommandExecutorResponseValidator;
import org.bremersee.dccon.repository.img.ImageScaler;
import org.bremersee.dccon.repository.ldap.DomainUserLdapMapper;
import org.bremersee.exception.ServiceException;
import org.ldaptive.AttributeModification;
import org.ldaptive.AttributeModificationType;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapException;
import org.ldaptive.ModifyRequest;
import org.ldaptive.ResultCode;
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchRequest;
import org.ldaptive.io.ByteArrayValueTranscoder;
import org.ldaptive.io.StringValueTranscoder;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
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

  private static final ResourceLoader RESOURCE_LOADER = new DefaultResourceLoader();

  private static final String NO_EMAIL_AVATAR = "classpath:mp.jpg";

  private static final String MIN_LENGTH_PLACEHOLDER = "{{MIN_LENGTH}}";

  private static final String SIMPLE_PASSWORD_REGEX = "^(?=.{" + MIN_LENGTH_PLACEHOLDER
      + ",75}$).*";

  private static final String COMPLEX_PASSWORD_REGEX = "(?=^.{" + MIN_LENGTH_PLACEHOLDER + ",75}$)"
      + "((?=.*\\d)(?=.*[A-Z])(?=.*[a-z])|(?=.*\\d)(?=.*[^A-Za-z0-9])(?=.*[a-z])"
      + "|(?=.*[^A-Za-z0-9])(?=.*[A-Z])(?=.*[a-z])|(?=.*\\d)(?=.*[A-Z])(?=.*[^A-Za-z0-9]))^.*";

  private final DomainRepository domainRepository;

  private final DomainGroupRepository domainGroupRepository;

  private LdaptiveEntryMapper<DomainUser> domainUserLdapMapper;

  /**
   * Instantiates a new domain user repository.
   *
   * @param properties            the properties
   * @param ldapTemplate          the ldap template
   * @param domainRepository      the domain repository
   * @param domainGroupRepository the domain group repository
   */
  public DomainUserRepositoryImpl(
      final DomainControllerProperties properties,
      final LdaptiveTemplate ldapTemplate,
      final DomainRepository domainRepository,
      final DomainGroupRepository domainGroupRepository) {
    super(properties, ldapTemplate);
    this.domainUserLdapMapper = new DomainUserLdapMapper(properties);
    this.domainRepository = domainRepository;
    this.domainGroupRepository = domainGroupRepository;
  }

  private Pattern getPasswordPattern() {
    final PasswordInformation info = domainRepository.getPasswordInformation();
    final String minLength = info.getMinimumPasswordLength() != null
        ? info.getMinimumPasswordLength().toString()
        : "7";
    final String regex;
    if (PasswordComplexity.OFF == info.getPasswordComplexity()) {
      regex = SIMPLE_PASSWORD_REGEX.replace(MIN_LENGTH_PLACEHOLDER, minLength);
    } else {
      regex = COMPLEX_PASSWORD_REGEX.replace(MIN_LENGTH_PLACEHOLDER, minLength);
    }
    return Pattern.compile(regex);
  }

  /**
   * Sets domain user ldap mapper.
   *
   * @param domainUserLdapMapper the domain user ldap mapper
   */
  @SuppressWarnings("unused")
  public void setDomainUserLdapMapper(
      final LdaptiveEntryMapper<DomainUser> domainUserLdapMapper) {
    if (domainUserLdapMapper != null) {
      this.domainUserLdapMapper = domainUserLdapMapper;
    }
  }

  @Override
  public Stream<DomainUser> findAll(final String query) {
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

  private boolean isQueryResult(final DomainUser domainUser, final String query) {
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
  public Optional<DomainUser> findOne(final String userName) {
    final SearchFilter searchFilter = new SearchFilter(getProperties().getUserFindOneFilter());
    searchFilter.setParameter(0, userName);
    final SearchRequest searchRequest = new SearchRequest(
        getProperties().getUserBaseDn(),
        searchFilter);
    searchRequest.setSearchScope(getProperties().getUserFindOneSearchScope());
    searchRequest.setBinaryAttributes(DomainUser.LDAP_ATTR_AVATAR);
    searchRequest.setSizeLimit(1L);
    return getLdapTemplate().findOne(searchRequest, domainUserLdapMapper);
  }

  @Override
  public Optional<byte[]> findAvatar(
      final String userName,
      final AvatarDefault avatarDefault,
      final Integer size) {

    final int avatarSize = size == null || size < 1 || size > 2048 ? 80 : size;
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
          final byte[] avatar = LdaptiveEntryMapper.getAttributeValue(
              ldapEntry, DomainUser.LDAP_ATTR_AVATAR, BYTE_ARRAY_VALUE_TRANSCODER, null);
          if (avatar != null && avatar.length > 0) {
            try {
              final BufferedImage img = ImageIO.read(new ByteArrayInputStream(avatar));
              final BufferedImage scaledImg = ImageScaler
                  .scaleImage(img, new Dimension(avatarSize, avatarSize));
              final ByteArrayOutputStream out = new ByteArrayOutputStream();
              ImageIO.write(scaledImg, "JPG", out);
              return out.toByteArray();

            } catch (IOException e) {
              log.error("msg=[Creating image from ldap attribute {} failed.]",
                  DomainUser.LDAP_ATTR_AVATAR, e);
            }
          }
          final String mail = LdaptiveEntryMapper
              .getAttributeValue(ldapEntry, "mail", STRING_VALUE_TRANSCODER, null);
          if (StringUtils.hasText(mail)) {
            final byte[] md5 = DigestUtils.md5Digest(mail.getBytes(StandardCharsets.UTF_8));
            final String hex = new String(Hex.encode(md5));
            final String defaultAvatar = avatarDefault != null
                ? avatarDefault.toString()
                : AvatarDefault.NOT_FOUND.toString();
            final String url = getProperties().getGravatarUrl()
                .replace("{hash}", hex)
                .replace("{default}", defaultAvatar)
                .replace("{size}", String.valueOf(avatarSize));
            try {
              return IOUtils.toByteArray(new URL(url));
            } catch (Exception e) {
              if (AvatarDefault.NOT_FOUND.toString().equalsIgnoreCase(defaultAvatar)) {
                return null;
              }
              log.error("msg=[Getting avatar failed. This should not happen.] url=[{}]",
                  url, e);
            }
          } else if (AvatarDefault.NOT_FOUND == avatarDefault) {
            return null;
          }
          try {
            final BufferedImage img = ImageIO
                .read(RESOURCE_LOADER.getResource(NO_EMAIL_AVATAR).getInputStream());
            final BufferedImage scaledImg = ImageScaler
                .scaleImage(img, new Dimension(avatarSize, avatarSize));
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(scaledImg, "JPG", out);
            return out.toByteArray();

          } catch (IOException e) {
            final ServiceException se = ServiceException.internalServerError(
                "Getting default avatar for no email failed.",
                "org.bremersee:dc-con-app:1ec0dda8-7358-4e1c-a8f2-f4bd64e439f0",
                e);
            log.error("msg=[{}]", se.getMessage(), se);
            throw se;
          }
        });
  }

  @Override
  public boolean exists(final String userName) {
    return getLdapTemplate()
        .exists(DomainUser.builder().userName(userName).build(), domainUserLdapMapper);
  }

  @Override
  public DomainUser save(final DomainUser domainUser, final Boolean updateGroups) {
    if (!exists(domainUser.getUserName())) {
      if (StringUtils.hasText(domainUser.getPassword())
          && !getPasswordPattern().matcher(domainUser.getPassword()).matches()) {
        throw ServiceException.badRequest(
            "msg=[The password does not meet the complexity criteria!] userName=["
                + domainUser.getUserName() + "]",
            "check_password_restrictions");
      }
      // Maybe I can add an user directly:
      // https://asadumar.wordpress.com/2013/02/28/create-user-password-in-active-directory-through-java-code/
      kinit();
      final List<String> commands = new ArrayList<>();
      sudo(commands);
      commands.add(getProperties().getSambaToolBinary());
      commands.add("user");
      commands.add("create");
      commands.add(domainUser.getUserName());
      commands.add("--random-password");
      commands.add("--use-username-as-cn");
      auth(commands);

      CommandExecutor.exec(
          commands,
          null,
          getProperties().getSambaToolExecDir(),
          (CommandExecutorResponseValidator) response -> {
            if (!exists(domainUser.getUserName())) {
              throw ServiceException.internalServerError("msg=[Saving user failed.] userName=["
                      + domainUser.getUserName() + "] "
                      + CommandExecutorResponse.toExceptionMessage(response),
                  "org.bremersee:dc-con-app:216e1246-b464-48f1-ac88-20e8461dea1e");
            }
          });
      if (StringUtils.hasText(domainUser.getPassword())) {
        savePassword(domainUser.getUserName(), domainUser.getPassword());
      }
    }

    final DomainUser updatedDomainUser = getLdapTemplate().save(domainUser, domainUserLdapMapper);
    if (Boolean.TRUE.equals(updateGroups)) {
      final Set<String> oldGroups = new HashSet<>(updatedDomainUser.getGroups());
      final Set<String> newGroups = new HashSet<>(domainUser.getGroups());
      for (final String newGroup : newGroups) {
        if (!oldGroups.remove(newGroup)) {
          domainGroupRepository.findOne(newGroup).ifPresent(group -> {
            group.getMembers().add(domainUser.getUserName());
            domainGroupRepository.save(group);
          });
        }
      }
      for (final String oldGroup : oldGroups) {
        domainGroupRepository.findOne(oldGroup).ifPresent(group -> {
          group.getMembers().remove(domainUser.getUserName());
          domainGroupRepository.save(group);
        });
      }
      updatedDomainUser.getGroups().clear();
      updatedDomainUser.getGroups().addAll(newGroups);
    } else {
      updatedDomainUser.setGroups(domainUser.getGroups());
    }
    updatedDomainUser.getGroups().sort(String::compareToIgnoreCase);
    return updatedDomainUser;
  }

  @Override
  public void savePassword(final String userName, final String newPassword) {
    final String quotedPassword = "\"" + newPassword + "\"";
    final char[] unicodePwd = quotedPassword.toCharArray();
    final byte[] pwdArray = new byte[unicodePwd.length * 2];
    for (int i = 0; i < unicodePwd.length; i++) {
      pwdArray[i * 2 + 1] = (byte) (unicodePwd[i] >>> 8);
      pwdArray[i * 2] = (byte) (unicodePwd[i] & 0xff);
    }
    final LdapAttribute ldapAttribute = new LdapAttribute(true);
    ldapAttribute.setName("unicodePwd");
    ldapAttribute.addBinaryValue(pwdArray);
    final AttributeModification attributeModification = new AttributeModification();
    attributeModification.setAttributeModificationType(AttributeModificationType.REPLACE);
    attributeModification.setAttribute(ldapAttribute);
    final String dn = LdaptiveEntryMapper.createDn(
        getProperties().getUserRdn(),
        userName,
        getProperties().getUserBaseDn());
    final ModifyRequest modifyRequest = new ModifyRequest();
    modifyRequest.setDn(dn);
    modifyRequest.setAttributeModifications(attributeModification);
    getLdapTemplate()
        .clone(new AbstractLdaptiveErrorHandler() {
          @Override
          public LdaptiveException map(final LdapException ldapException) {
            final HttpStatus httpStatus;
            final String errorCode;
            if (ldapException.getResultCode() == ResultCode.CONSTRAINT_VIOLATION
                && ldapException.getMessage().contains("check_password_restrictions")) {
              httpStatus = HttpStatus.BAD_REQUEST;
              errorCode = "check_password_restrictions";
            } else {
              httpStatus = ldapException.getResultCode() == ResultCode.NO_SUCH_OBJECT
                  ? HttpStatus.NOT_FOUND
                  : HttpStatus.INTERNAL_SERVER_ERROR;
              errorCode = "org.bremersee.dc-con-app:a70939fb-2c94-412f-80c0-00a7d5dcf4a6";
            }
            return new LdaptiveException(httpStatus, errorCode, ldapException);
          }
        })
        .modify(modifyRequest);
  }

  @Override
  public boolean delete(final String userName) {

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
