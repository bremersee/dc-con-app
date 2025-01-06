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

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNullElse;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.AvatarDefault;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.model.Sid;
import org.bremersee.dccon.repository.automock.MockComponent;
import org.bremersee.dccon.repository.automock.ProfileRequired;
import org.bremersee.dccon.repository.cli.CommandExecutor;
import org.bremersee.dccon.repository.cli.CommandExecutorResponse;
import org.bremersee.dccon.repository.cli.CommandExecutorResponseValidator;
import org.bremersee.dccon.repository.img.ImageUtils;
import org.bremersee.exception.ServiceException;
import org.bremersee.ldaptive.AbstractLdaptiveErrorHandler;
import org.bremersee.ldaptive.LdaptiveEntryMapper;
import org.bremersee.ldaptive.LdaptiveException;
import org.bremersee.ldaptive.LdaptiveTemplate;
import org.ldaptive.AttributeModification;
import org.ldaptive.AttributeModification.Type;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.ModifyRequest;
import org.ldaptive.ResultCode;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
import org.ldaptive.dn.NameValue;
import org.ldaptive.dn.RDn;
import org.ldaptive.filter.AndFilter;
import org.ldaptive.filter.EqualityFilter;
import org.ldaptive.filter.Filter;
import org.ldaptive.filter.NotFilter;
import org.ldaptive.filter.OrFilter;
import org.ldaptive.filter.SubstringFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * The domain user repository.
 *
 * @author Christian Bremer
 */
@Primary
@Component("domainUserRepository")
@ProfileRequired("ldap")
@MockComponent(value = DomainUserRepositoryMock.class, methodsOf = DomainUserRepository.class)
@Slf4j
public class DomainUserRepositoryImpl extends AbstractDomainUserRepository {

  private final LdaptiveEntryMapper<DomainUser> domainUserLdapMapper;

  /**
   * Instantiates a new domain user repository.
   *
   * @param properties the properties
   * @param ldapTemplateProvider the ldap template provider
   * @param domainRepository the domain repository
   * @param avatarProviders the avatar repositories
   */
  public DomainUserRepositoryImpl(
      DomainControllerProperties properties,
      ObjectProvider<LdaptiveTemplate> ldapTemplateProvider,
      LdaptiveEntryMapper<DomainUser> domainUserLdapMapper,
      DomainRepository domainRepository,
      List<AvatarProvider> avatarProviders) {
    super(properties, ldapTemplateProvider.getIfAvailable(), domainRepository, avatarProviders);
    this.domainUserLdapMapper = domainUserLdapMapper;
  }

  @Override
  Filter objectClassFilter() {
    Filter objectClassFilter = new EqualityFilter(LDAP_OBJECT_CLASS, LDAP_OBJECT_CLASS_USER);
    Filter noComputerFilter = new NotFilter(
        new EqualityFilter(LDAP_OBJECT_CLASS, LDAP_OBJECT_CLASS_COMPUTER));
    return new AndFilter(objectClassFilter, noComputerFilter);
  }

  private Filter getFindAllFilter(String query) {
    //noinspection DuplicatedCode
    Filter objectClassFilter = objectClassFilter();
    if (isNull(query) || query.length() <= 2) {
      return objectClassFilter;
    }
    Filter orFilter = new OrFilter(
        new SubstringFilter(LDAP_USER_COMPANY, null, null, query),
        new SubstringFilter(LDAP_USER_DEPARTMENT, null, null, query),
        new SubstringFilter(LDAP_DESCRIPTION, null, null, query),
        new SubstringFilter(LDAP_USER_DISPLAY_NAME, null, null, query),
        new EqualityFilter(LDAP_GID_NUMBER, query),
        new SubstringFilter(LDAP_USER_GIVEN_NAME, null, null, query),
        new SubstringFilter(LDAP_MAIL, null, null, query),
        new SubstringFilter(LDAP_USER_MOBILE, null, null, query),
        new SubstringFilter(LDAP_USER_OFFICE_NAME, null, null,
            query),
        new SubstringFilter(LDAP_SAM_ACCOUNT_NAME, null, null, query),
        new SubstringFilter(LDAP_USER_SN, null, null, query),
        new SubstringFilter(LDAP_USER_TELEPHONE_NUMBER, null, null, query),
        new EqualityFilter(LDAP_USER_UID_NUMBER, query)
    );
    return new AndFilter(objectClassFilter, orFilter);
  }

  @Override
  public Stream<DomainUser> findAll(String query, Dn ou, SearchScope searchScope) {
    log.debug("findAll({}, {}, {})", query, ou, searchScope);
    SearchRequest searchRequest = searchAllRequest(
        ou,
        getFindAllFilter(query),
        searchScope,
        getReturnAttributes());
    log.debug("findAll, searchRequest = {}", searchRequest);
    return getLdapTemplate()
        .findAll(searchRequest, domainUserLdapMapper)
        .filter(getIgnoredObjectFilter(ou, searchScope));
  }

  @Override
  public Optional<DomainUser> findOne(String userName, Dn ou, SearchScope searchScope) {
    log.debug("findOne({})", userName);
    SearchRequest searchRequest = searchOneRequest(userName, ou, searchScope);
    log.debug("findOne, searchRequest = {}", searchRequest);
    return getLdapTemplate()
        .findOne(searchRequest, domainUserLdapMapper)
        .filter(getIgnoredObjectFilter(ou, searchScope));
  }

  @Override
  public boolean existsAvatarInActiveDirectory(
      String user,
      Dn ou,
      SearchScope searchScope) {

    log.debug("existsAvatarInActiveDirectory({}, {}, {})", user, ou, searchScope);
    return findLdapEntryForAvatar(user, ou, searchScope)
        .map(ldapEntry -> ldapEntry.getAttribute(LDAP_USER_JPEG_PHOTO))
        .map(LdapAttribute::getBinaryValue)
        .map(this::isAvatarNotEmpty)
        .orElse(false);
  }

  @Override
  public Optional<byte[]> findAvatar(
      String userNameOrEmail,
      Dn ou,
      SearchScope searchScope,
      AvatarDefault avatarDefault,
      Integer size) {

    log.debug("findAvatar({}, {}, {})", userNameOrEmail, avatarDefault, size);
    int avatarSize = getAvatarSize(size);
    return findLdapEntryForAvatar(userNameOrEmail, ou, searchScope)
        .map(ldapEntry -> {
          byte[] avatar = LdaptiveEntryMapper.getAttributeValue(
              ldapEntry, LDAP_USER_JPEG_PHOTO, BYTE_ARRAY_VALUE_TRANSCODER, null);
          if (avatar != null && avatar.length > 0) {
            try {
              BufferedImage img = ImageUtils.toSquareImage(avatar);
              BufferedImage scaledImg = ImageUtils
                  .scaleImage(img, new Dimension(avatarSize, avatarSize));
              ByteArrayOutputStream out = new ByteArrayOutputStream();
              ImageIO.write(scaledImg, "JPG", out);
              avatar = out.toByteArray();

            } catch (IOException e) {
              log.error("Creating image from ldap attribute {} failed.",
                  LDAP_USER_JPEG_PHOTO, e);
              avatar = null;
            }
          }
          if (avatar == null) {
            log.debug("Avatar not found in active directory.");
            String mail = LdaptiveEntryMapper.getAttributeValue(
                ldapEntry, LDAP_MAIL, STRING_VALUE_TRANSCODER, userNameOrEmail);
            avatar = findAvatarsOfProviders(mail, avatarDefault, avatarSize)
                .findFirst()
                .orElse(null);
          }
          return avatar;
        })
        .filter(this::isAvatarNotEmpty)
        .or(() -> findAvatarsOfProviders(userNameOrEmail, avatarDefault, avatarSize)
            .findFirst());
  }

  private Optional<LdapEntry> findLdapEntryForAvatar(
      String userNameOrEmail,
      Dn ou,
      SearchScope searchScope) {

    if (isEmpty(userNameOrEmail)) {
      log.debug("Avatar not found because userNameOrEmail is empty.");
      return Optional.empty();
    }
    Filter objectClassFilter = new EqualityFilter(LDAP_OBJECT_CLASS, getObjectClassValue());
    Filter nameFilter = new EqualityFilter(getUniqueNameAttributeName(), userNameOrEmail);
    Filter emailFilter = new EqualityFilter(LDAP_MAIL, userNameOrEmail);
    Filter orFilter = new OrFilter(nameFilter, emailFilter);
    Filter filter = new AndFilter(objectClassFilter, orFilter);
    SearchRequest searchRequest = searchOneRequest(userNameOrEmail, ou, filter, searchScope,
        LDAP_USER_JPEG_PHOTO, LDAP_MAIL);
    log.debug("findAvatar, searchRequest = {}", searchRequest);
    return getLdapTemplate()
        .findOne(searchRequest)
        .filter(getIgnoredEntryFilter(ou, searchScope));
  }

  @Override
  public void removeAvatar(String userName) {
    log.debug("removeAvatar({})", userName);
    LdapAttribute ldapAttribute = new LdapAttribute();
    ldapAttribute.setName(LDAP_USER_JPEG_PHOTO);
    ldapAttribute.setBinary(true);
    modifyAvatar(userName, ldapAttribute, Type.DELETE);
  }

  @Override
  public void saveAvatar(String userName, InputStream avatar) {
    log.debug("saveAvatar({}, InputStream)", userName);
    LdapAttribute ldapAttribute = new LdapAttribute();
    ldapAttribute.setName(LDAP_USER_JPEG_PHOTO);
    ldapAttribute.setBinary(true);
    try (InputStream in = avatar) {
      BufferedImage img = ImageIO.read(in);
      int width = img.getWidth();
      int height = img.getHeight();
      int max = Math.max(width, height);
      if (max > MAX_AVATAR_SIZE) {
        float factor = Integer.valueOf(MAX_AVATAR_SIZE).floatValue() / max;
        width = Math.min(Math.round(factor * width), MAX_AVATAR_SIZE);
        height = Math.min(Math.round(factor * height), MAX_AVATAR_SIZE);
        img = ImageUtils.scaleImage(img, new Dimension(width, height));
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ImageIO.write(img, "jpg", out);
      ldapAttribute.addBinaryValues(out.toByteArray());
    } catch (IOException e) {
      throw ServiceException.internalServerError(
          "Saving avatar failed.",
          EC_SAVING_AVATAR_FAILED,
          e);
    }
    modifyAvatar(userName, ldapAttribute, AttributeModification.Type.REPLACE);
  }

  private void modifyAvatar(
      String userName,
      LdapAttribute ldapAttribute,
      AttributeModification.Type modificationType) {

    AttributeModification attributeModification = new AttributeModification(modificationType,
        ldapAttribute);
    getDomainRepository().findDnOfSamAccountName(userName).ifPresentOrElse(
        dn -> {
          ModifyRequest modifyRequest = ModifyRequest.builder()
              .dn(dn)
              .modifications(attributeModification)
              .build();
          getLdapTemplate().modify(modifyRequest);
        },
        () -> {
          throw ServiceException.notFoundWithErrorCode(
              DomainUser.class.getSimpleName(),
              userName,
              EC_SAM_ACCOUNT_NOT_FOUND);
        });
  }

  /**
   * Add user.
   *
   * @param domainUser the domain user
   */
  String doAdd(DomainUser domainUser, Dn ou, Boolean useUsernameAsCn) {
    log.debug("doAdd({}, {}, {})", domainUser.getSamAccountName(),
        Optional.ofNullable(ou).map(Dn::format).orElse(null), useUsernameAsCn);
    Dn userOu = getProperties().removeBaseDn(validateOu(ou));
    log.debug("doAdd({}, {}, {})", domainUser.getSamAccountName(),
        Optional.ofNullable(userOu).map(Dn::format).orElse(null), useUsernameAsCn);
    boolean usernameAsCn = requireNonNullElse(
        useUsernameAsCn, getProperties().getUser().isUseUsernameAsCn());
    kinit();
    List<String> commands = new ArrayList<>();
    ssh(commands);
    sudo(commands);
    commands.add(getProperties().getSambaToolBinary());
    commands.add("user");
    commands.add("add");
    commands.add(quote(domainUser.getSamAccountName()));
    commands.add("--random-password");
    if (!isEmpty(userOu)) {
      commands.add("--userou=" + quote(userOu.format()));
    }
    if (usernameAsCn || isEmpty(domainUser.getFirstName()) || isEmpty(domainUser.getLastName())) {
      commands.add("--use-username-as-cn");
    }
    if (!isEmpty(domainUser.getLastName())) {
      commands.add("--surname=" + quote(domainUser.getLastName()));
    }
    if (!isEmpty(domainUser.getFirstName())) {
      commands.add("--given-name=" + quote(domainUser.getFirstName()));
    }
    if (getDomainRepository().isRfc2307Enabled() && hasAllNisAttributes(domainUser)) {
      commands.add("--nis-domain=" + quote(getNisDomain(domainUser)));
      commands.add("--uidNumber=" + domainUser.getUidNumber());
      commands.add("--login-shell=" + quote(domainUser.getLoginShell()));
      commands.add("--unix-home=" + quote(domainUser.getUnixHomeDirectory()));
      commands.add("--gid-number=" + domainUser.getGidNumber());
      commands.add("--uid=" + quote(domainUser.getSamAccountName()));
    }
    auth(commands);

    return CommandExecutor.exec(
        commands,
        null,
        getProperties().getSambaToolExecDir(),
        response -> getDomainRepository().findDnOfSamAccount(domainUser)
            .orElseThrow(() -> ServiceException
                .internalServerError(String.format("Adding user '%s' failed. %s",
                        domainUser.getSamAccountName(),
                        CommandExecutorResponse.toExceptionMessage(response)),
                    EC_ADDING_USER_FAILED)));
  }

  // TODO ERROR: Missing parameters. To enable NIS features, the following options have to be given: --nis-domain=, --uidNumber=, --login-shell=, --unix-home=, --gid-number= Operation cancelled.
  public boolean hasAllNisAttributes(DomainUser domainUser) {
    return !isEmpty(domainUser)
        && !isEmpty(getNisDomain(domainUser))
        && !isEmpty(domainUser.getUidNumber())
        && !isEmpty(domainUser.getLoginShell())
        && !isEmpty(domainUser.getUnixHomeDirectory())
        && !isEmpty(domainUser.getGidNumber());
  }

  @ProfileRequired({"cli", "ldap"})
  @Override
  public DomainUser add(DomainUser domainUser, Dn ou, Boolean useUsernameAsCn) {
    log.debug("add({}, {}, {})", domainUser.getSamAccountName(), ou, useUsernameAsCn);
    if (isEmpty(domainUser.getSamAccountName())) {
      throw ServiceException.badRequest(
          "Username (samAccountName) is required.",
          EC_SAM_ACCOUNT_NAME_REQUIRED);
    }
    if (getDomainRepository().samAccountNameExists(domainUser.getSamAccountName())) {
      throw ServiceException.alreadyExistsWithErrorCode(
          DomainUser.class.getSimpleName(),
          domainUser.getSamAccountName(),
          EC_SAM_ACCOUNT_ALREADY_EXISTS);
    }
    if (!isEmpty(domainUser.getPassword())
        && !getPasswordPattern().matcher(domainUser.getPassword()).matches()) {
      throw ServiceException.badRequest(
          String.format(
              "The password of user '%s' does not meet the complexity criteria!",
              domainUser.getSamAccountName()),
          EC_PASSWORD_RESTRICTIONS);
    }
    String dn = doAdd(domainUser, ou, useUsernameAsCn);
    if (!isEmpty(domainUser.getPassword())) {
      doSavePassword(dn, domainUser.getPassword());
    }
    domainUser.setDistinguishedName(dn);
    return getLdapTemplate().save(domainUser, domainUserLdapMapper);
    /*
    DomainUser updatedDomainUser = getLdapTemplate().save(domainUser, domainUserLdapMapper);
    if (Boolean.TRUE.equals(updateGroups)) {
      Set<String> oldGroups = new HashSet<>(updatedDomainUser.getGroups());
      Set<String> newGroups = new HashSet<>(domainUser.getGroups());
      for (String newGroup : newGroups) {
        if (!oldGroups.remove(newGroup)) {
          domainGroupRepository.findOne(newGroup).ifPresent(group -> {
            group.getMembers().add(domainUser.getSamAccountName());
            domainGroupRepository.save(group);
          });
        }
      }
      for (String oldGroup : oldGroups) {
        domainGroupRepository.findOne(oldGroup).ifPresent(group -> {
          group.getMembers().remove(domainUser.getSamAccountName());
          domainGroupRepository.save(group);
        });
      }
      updatedDomainUser = updatedDomainUser.toBuilder()
          .groups(new ArrayList<>(newGroups))
          .build();
    } else {
      updatedDomainUser = updatedDomainUser.toBuilder()
          .groups(new ArrayList<>(domainUser.getGroups()))
          .build();
    }
    updatedDomainUser.getGroups().sort(String::compareToIgnoreCase);
    return updatedDomainUser;
    */
  }

  @Override
  public DomainUser update(DomainUser domainUser) {
    log.debug("update({})", domainUser);
    return getDomainRepository().findDnOfSamAccount(domainUser)
        .map(dn -> validateDn(domainUser, dn))
        .map(dn -> getLdapTemplate().save(domainUser, domainUserLdapMapper))
        .orElseThrow(() -> ServiceException.notFoundWithErrorCode(
            DomainUser.class.getSimpleName(),
            domainUser.getSamAccountName(),
            EC_SAM_ACCOUNT_NOT_FOUND));
  }

  @ProfileRequired({"cli", "ldap"})
  @Override
  public DomainUser update(String userName, DomainUser domainUser, Dn newOu) {
    log.debug("update({}, {}, {})", userName, domainUser.getSamAccountName(), newOu);
    if (isEmpty(domainUser.getSamAccountName())) {
      throw ServiceException.badRequest(
          "Username (samAccountName) is required.",
          EC_SAM_ACCOUNT_NAME_REQUIRED);
    }
    if (!userName.equalsIgnoreCase(domainUser.getSamAccountName())
        && getDomainRepository().samAccountNameExists(domainUser.getSamAccountName())) {
      throw ServiceException.alreadyExistsWithErrorCode(
          DomainUser.class.getSimpleName(),
          domainUser.getSamAccountName(),
          EC_SAM_ACCOUNT_ALREADY_EXISTS);
    }
    Dn currentParentDn = getProperties().getParentDn(domainUser.getDistinguishedName());
    DomainUser existingDomainUser = findOne(userName, currentParentDn, SearchScope.ONELEVEL)
        .orElseThrow(() -> ServiceException.notFoundWithErrorCode(
            DomainUser.class.getSimpleName(),
            domainUser.getSamAccountName(),
            EC_SAM_ACCOUNT_NOT_FOUND));
    Dn oldDn = new Dn(existingDomainUser.getDistinguishedName());
    Dn newDn = getNewDn(existingDomainUser, domainUser, newOu);
    if (!oldDn.isSame(newDn) && getDomainRepository().dnExistsWithAnyObjectClass(newDn.format())) {
      throw ServiceException.alreadyExistsWithErrorCode(
          DomainUser.class.getSimpleName(),
          getProperties().removeBaseDn(newDn),
          EC_DN_ALREADY_EXISTS);
    }

    DomainUser updatedDomainUser = renameAndMove(existingDomainUser, domainUser, newDn);
    return getLdapTemplate().save(updatedDomainUser, domainUserLdapMapper);
  }

  Dn getNewDn(DomainUser oldDomainUser, DomainUser newDomainUser, Dn newOu) {
    Dn newParentDn;
    if (!isEmpty(newOu) && !newOu.isEmpty()) {
      newParentDn = getProperties().getBaseDn(validateOu(newOu));
    } else {
      newParentDn = getProperties().getParentDn(oldDomainUser.getDistinguishedName());
    }

    RDn oldRdn = new Dn(oldDomainUser.getDistinguishedName()).getRDn();
    String oldCn = oldRdn.getNameValue().getStringValue().toLowerCase();
    String newCn;
    if (oldCn.equalsIgnoreCase(newDomainUser.getSamAccountName())
        || oldCn.equalsIgnoreCase(newDomainUser.getDisplayName())) {
      newCn = oldCn;
    } else if (oldCn.equalsIgnoreCase(oldDomainUser.getDisplayName())
        && !isEmpty(newDomainUser.getFirstName()) && !isEmpty(newDomainUser.getLastName())) {
      newCn = newDomainUser.getFirstName() + " " + newDomainUser.getLastName();
    } else {
      newCn = newDomainUser.getSamAccountName();
    }
    Dn newDn = new Dn(new RDn(new NameValue(oldRdn.getNameValue().getName(), newCn)));
    newDn.add(newParentDn);
    return newDn;
  }

  DomainUser renameAndMove(DomainUser oldDomainUser, DomainUser newDomainUser, Dn newDn) {
    String oldCn = new Dn(oldDomainUser.getDistinguishedName())
        .getRDn().getNameValue().getStringValue();
    String newCn = newDn
        .getRDn().getNameValue().getStringValue();
    Dn oldParentDn = getProperties().getParentDn(oldDomainUser.getDistinguishedName());
    String oldSamAccountName = oldDomainUser.getSamAccountName();
    String newSamAccountName = newDomainUser.getSamAccountName();
    if (!oldCn.equals(newCn) || !oldSamAccountName.equals(newSamAccountName)) {
      kinit();
      List<String> commands = new ArrayList<>();
      ssh(commands);
      sudo(commands);
      commands.add(getProperties().getSambaToolBinary());
      commands.add("user");
      commands.add("rename");
      commands.add(quote(oldSamAccountName));
      commands.add("--samaccountname=" + quote(newSamAccountName));
      commands.add("--force-new-cn=" + quote(newCn));
      auth(commands);
      CommandExecutor.exec(
          commands,
          null,
          getProperties().getSambaToolExecDir(),
          (CommandExecutorResponseValidator) response -> this
              .findOne(
                  newSamAccountName,
                  oldParentDn,
                  SearchScope.ONELEVEL)
              .orElseThrow(() -> ServiceException
                  .internalServerError(String.format("Updating names of user '%s' failed. %s",
                          newDomainUser.getSamAccountName(),
                          CommandExecutorResponse.toExceptionMessage(response)),
                      EC_UPDATING_USER_FAILED)));
    }
    Dn newParentDn = getProperties().getParentDn(newDn.format());
    if (!oldParentDn.isSame(newParentDn)) {
      String ou = getProperties().removeBaseDn(newParentDn).format();
      kinit();
      List<String> commands = new ArrayList<>();
      ssh(commands);
      sudo(commands);
      commands.add(getProperties().getSambaToolBinary());
      commands.add("user");
      commands.add("move");
      commands.add(quote(newSamAccountName));
      commands.add(quote(ou));
      auth(commands);

      CommandExecutor.exec(
          commands,
          null,
          getProperties().getSambaToolExecDir(),
          (CommandExecutorResponseValidator) response -> getDomainRepository()
              .findDnOfSamAccountName(newSamAccountName)
              .filter(userDn -> new Dn(userDn).isSame(newDn))
              .orElseThrow(() -> ServiceException
                  .internalServerError(String.format("Moving user '%s' to '%s' failed. %s",
                          newSamAccountName, ou,
                          CommandExecutorResponse.toExceptionMessage(response)),
                      EC_UPDATING_USER_FAILED)));
    }
    newDomainUser.setDistinguishedName(newDn.format());
    return newDomainUser;
  }

  @Override
  public void savePassword(String userName, String newPassword) {
    log.debug("savePassword({}, ****)", userName);
    getDomainRepository().findDnOfSamAccountName(userName).ifPresentOrElse(
        dn -> doSavePassword(dn, newPassword),
        () -> {
          throw ServiceException.notFoundWithErrorCode(
              DomainUser.class.getSimpleName(),
              userName,
              EC_SAM_ACCOUNT_NOT_FOUND);
        });
  }

  void doSavePassword(String dn, String newPassword) {
    // https://asadumar.wordpress.com/2013/02/28/create-user-password-in-active-directory-through-java-code/
    String quotedPassword = "\"" + newPassword + "\"";
    char[] unicodePwd = quotedPassword.toCharArray();
    byte[] pwdArray = new byte[unicodePwd.length * 2];
    for (int i = 0; i < unicodePwd.length; i++) {
      pwdArray[i * 2 + 1] = (byte) (unicodePwd[i] >>> 8);
      pwdArray[i * 2] = (byte) (unicodePwd[i] & 0xff);
    }
    LdapAttribute ldapAttribute = new LdapAttribute();
    ldapAttribute.setName(LDAP_USER_UNICODE_PWD);
    ldapAttribute.setBinary(true);
    ldapAttribute.addBinaryValues(pwdArray);
    AttributeModification attributeModification = new AttributeModification(Type.REPLACE,
        ldapAttribute);
    ModifyRequest modifyRequest = ModifyRequest.builder()
        .dn(dn)
        .modifications(attributeModification)
        .build();
    getLdapTemplate()
        .clone(new AbstractLdaptiveErrorHandler() {
          @Override
          public LdaptiveException map(LdapException ldapException) {
            HttpStatus httpStatus;
            String errorCode;
            if (ldapException.getResultCode() == ResultCode.CONSTRAINT_VIOLATION
                && ldapException.getMessage().contains("check_password_restrictions")) {
              httpStatus = HttpStatus.BAD_REQUEST;
              errorCode = "check_password_restrictions";
            } else {
              httpStatus = ldapException.getResultCode() == ResultCode.NO_SUCH_OBJECT
                  ? HttpStatus.NOT_FOUND
                  : HttpStatus.INTERNAL_SERVER_ERROR;
              errorCode = httpStatus == HttpStatus.NOT_FOUND
                  ? EC_SAM_ACCOUNT_NOT_FOUND
                  : EC_SAVING_PASSWORD_FAILED;
            }
            return LdaptiveException.builder()
                .httpStatus(httpStatus.value())
                .errorCode(errorCode)
                .cause(ldapException)
                .build();
          }
        })
        .modify(modifyRequest);
  }

  @ProfileRequired({"cli", "ldap"})
  @Override
  public boolean delete(String userName) {
    log.debug("delete({})", userName);
    return findOne(userName, null, null)
        .filter(user -> Optional.ofNullable(user.getSid())
            .map(Sid::getSystemEntity)
            .orElse(false))
        .map(user -> doDelete(user.getSamAccountName()))
        .orElse(false);
  }

  /**
   * Delete user.
   *
   * @param userName the username
   */
  boolean doDelete(String userName) {
    kinit();
    List<String> commands = new ArrayList<>();
    ssh(commands);
    sudo(commands);
    commands.add(getProperties().getSambaToolBinary());
    commands.add("user");
    commands.add("delete");
    commands.add(quote(userName));
    auth(commands);
    return CommandExecutor.exec(
        commands,
        null,
        getProperties().getSambaToolExecDir(),
        response -> {
          if (getDomainRepository().samAccountNameExists(userName)) {
            throw ServiceException.internalServerError(
                String.format("Deleting user '%s' failed: %s", userName,
                    CommandExecutorResponse.toExceptionMessage(response)),
                EC_DELETING_USER_FAILED);
          }
          return true;
        });
  }

}
