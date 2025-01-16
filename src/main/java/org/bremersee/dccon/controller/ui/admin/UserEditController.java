/*
 * Copyright 2024 the original author or authors.
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

package org.bremersee.dccon.controller.ui.admin;

import static java.util.Objects.requireNonNullElse;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.components.OrganisationalUnitsComponent;
import org.bremersee.dccon.controller.ui.components.OrganizationalUnitComponent;
import org.bremersee.dccon.controller.ui.components.PageableComponent;
import org.bremersee.dccon.controller.ui.model.DomainUserEditRequest;
import org.bremersee.dccon.controller.ui.model.RedirectMessage;
import org.bremersee.dccon.controller.ui.model.RedirectMessageType;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.model.TreeSearchScope;
import org.bremersee.dccon.service.DomainGroupService;
import org.bremersee.dccon.service.DomainService;
import org.bremersee.dccon.service.DomainUserService;
import org.bremersee.dccon.service.OrganizationalUnitService;
import org.bremersee.exception.ServiceException;
import org.ldaptive.dn.Dn;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The type UsersController.
 *
 * @author Christian Bremer
 */
@Controller
public class UserEditController extends AbstractEditController implements PageableComponent,
    OrganizationalUnitComponent, OrganisationalUnitsComponent {

  private final DomainService domainService;

  private final DomainUserService domainUserService;

  private final DomainGroupService domainGroupService;

  @Getter
  private final OrganizationalUnitService organizationalUnitService;

  public UserEditController(
      DomainControllerProperties domainControllerProperties,
      LocaleResolver localeResolver,
      DomainService domainService,
      DomainUserService domainUserService,
      DomainGroupService domainGroupService,
      OrganizationalUnitService organizationalUnitService) {
    super(domainControllerProperties, localeResolver);
    this.domainService = domainService;
    this.domainUserService = domainUserService;
    this.domainGroupService = domainGroupService;
    this.organizationalUnitService = organizationalUnitService;
  }

  @Override
  public String getDefaultSort() {
    return USER_SORT;
  }

  @ModelAttribute("rfc2307Enabled")
  public boolean isRfc2307Enabled() {
    return domainService.isRfc2307Enabled();
  }

  @ModelAttribute("avatarExists")
  public boolean avatarExists(@RequestParam(value = "user", required = false) String userName) {
    return Optional.ofNullable(userName)
        .map(user -> domainUserService.existsAvatarInActiveDirectory(user, null, null))
        .orElse(false);
  }

  @GetMapping(path = "/admin/user-edit")
  public String displayUserEdit(
      @RequestParam(value = "user", required = false) String userName,
      @RequestParam(value = OU, required = false) Dn ou,
      @RequestParam(value = SCOPE, required = false) TreeSearchScope searchScope,
      ModelMap model,
      RedirectAttributes redirectAttributes) {

    return Optional.ofNullable(userName)
        .flatMap(name -> domainUserService.getUser(userName, ou, searchScope))
        .map(user -> {
          model.addAttribute("user", user);
          List<DomainGroup> groups = domainGroupService.getMemberships(userName, ou, searchScope)
              .toList();
          model.addAttribute("groups", groups);
          DomainUserEditRequest request = DomainUserEditRequest.MAPPER.map(user);
          model.addAttribute("userEditRequest", request);
          return "admin/user-edit";
        })
        .orElseGet(() -> entityNotFoundRedirect(
            redirectAttributes, "User", "todo", userName, "users"));
  }

  @PostMapping(path = "/admin/user-edit")
  public String updateUser(
      @RequestParam(value = "user", required = false) String oldSamAccountName,
      @RequestParam(value = "previousSamAccountName", required = false) String previousSamAccountName,
      @RequestParam(value = "previousFirstName", required = false) String previousFirstName,
      @RequestParam(value = "previousLastName", required = false) String previousLastName,
      @RequestParam(value = OU, required = false) Dn ou,
      @RequestParam(value = SCOPE, required = false) TreeSearchScope searchScope,
      @ModelAttribute(name = "userEditRequest") DomainUserEditRequest userEditRequest,
      ModelMap model,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {

    getLogger().debug("updateUser({})", userEditRequest);

    if (userEditRequest.isRenameNamesAutomatically()) {
      replaceNames(userEditRequest, previousSamAccountName, userEditRequest.getSamAccountName());
      replaceNames(userEditRequest, previousFirstName, userEditRequest.getFirstName());
      replaceNames(userEditRequest, previousLastName, userEditRequest.getLastName());
    }

    return Optional.ofNullable(oldSamAccountName)
        .or(() -> Optional.ofNullable(userEditRequest.getSamAccountName()))
        .flatMap(oldName -> domainUserService.getUser(oldName, ou, searchScope))
        .map(existingUser -> updateUser(
            existingUser, userEditRequest, model, bindingResult, redirectAttributes))
        .orElseGet(() -> entityNotFoundRedirect(
            redirectAttributes, "User", "todo", oldSamAccountName, "users"));
  }

  private String updateUser(
      DomainUser existingUser,
      DomainUserEditRequest userEditRequest,
      ModelMap model,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {

    String oldSamAccountName = existingUser.getSamAccountName();
    DomainUserEditRequest.MAPPER.update(existingUser, userEditRequest);
    Dn ou = userEditRequest.getNewOuDn();
    DomainUser updatedUser = null;
    try {
      Dn parentDn = existingUser.getDn().getParent();
      Dn ouDn = getProperties().getBaseDn(ou);
      Dn newOu = parentDn.isSame(ouDn) ? null : ouDn;
      updatedUser = domainUserService.updateUser(oldSamAccountName, existingUser, newOu);
      updateAvatar(bindingResult, userEditRequest);

      model.clear();
      String defaultMsg = String
          .format("User '%s' was successfully updated.", updatedUser.getName());
      RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.SUCCESS, defaultMsg,
          "todo", updatedUser.getName());
      redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);

      Map<String, Object> parameters = getParamterMap(updatedUser.getDn().getParent());
      String redirect = getRedirectUri("user-edit?user={{user.samAccountName}}",
          PAGE_AND_OU_PARAMS, putToParameterMap(parameters, "user", updatedUser));
      logRedirectTo("User successfully updated.", redirect);
      return redirect;

    } catch (ServiceException e) {
      handleException(bindingResult, e);
      getLogger().debug("Updating user failed. Some fields were invalid.");
      String currentSamAccountName = Optional.ofNullable(updatedUser)
          .map(DomainUser::getSamAccountName)
          .orElse(oldSamAccountName);
      Dn currentOu = Optional.ofNullable(updatedUser)
          .map(DomainUser::getDn)
          .map(Dn::getParent)
          .orElseGet(() -> existingUser.getDn().getParent());
      DomainUser currentUser = domainUserService
          .getUser(currentSamAccountName, currentOu, TreeSearchScope.ONELEVEL)
          .orElseThrow(() -> ServiceException.internalServerError(String
              .format("Domain user '%s' was not found.", currentSamAccountName)));
      model.addAttribute("user", currentUser);
      boolean avatarExists = domainUserService.existsAvatarInActiveDirectory(
          currentSamAccountName, currentOu, TreeSearchScope.ONELEVEL);
      model.addAttribute("avatarExists", avatarExists);
      List<DomainGroup> groups = domainGroupService
          .getMemberships(currentSamAccountName, currentOu, TreeSearchScope.ONELEVEL)
          .toList();
      model.addAttribute("groups", groups);
      return "admin/user-edit";
    }
  }

  public void replaceNames(DomainUserEditRequest userEditRequest, String oldName, String newName) {
    if (isEmpty(userEditRequest) || isEmpty(oldName)) {
      return;
    }
    String replacement = isEmpty(newName) ? "" : newName;
    if (!isEmpty(userEditRequest.getDisplayName())) {
      userEditRequest.setDisplayName(userEditRequest.getDisplayName().replace(oldName, replacement).trim());
    }
    if (!isEmpty(userEditRequest.getGecos())) {
      userEditRequest.setGecos(userEditRequest.getGecos().replace(oldName, replacement).trim());
    }
    if (!isEmpty(userEditRequest.getHomeDirectory())) {
      userEditRequest.setHomeDirectory(
          userEditRequest.getHomeDirectory().replace(oldName, replacement).trim());
    }
    if (!isEmpty(userEditRequest.getProfilePath())) {
      userEditRequest.setProfilePath(userEditRequest.getProfilePath().replace(oldName, replacement).trim());
    }
    if (!isEmpty(userEditRequest.getScriptPath())) {
      userEditRequest.setScriptPath(userEditRequest.getScriptPath().replace(oldName, replacement).trim());
    }
    if (!isEmpty(userEditRequest.getUid())) {
      userEditRequest.setUid(userEditRequest.getUid().replace(oldName, replacement).trim());
    }
    if (!isEmpty(userEditRequest.getUnixHomeDirectory())) {
      userEditRequest.setUnixHomeDirectory(
          userEditRequest.getUnixHomeDirectory().replace(oldName, replacement).trim());
    }
    if (!isEmpty(userEditRequest.getUserPrincipalName())) {
      userEditRequest.setUserPrincipalName(
          userEditRequest.getUserPrincipalName().replace(oldName, replacement).trim());
    }
  }

  private void updateAvatar(BindingResult bindingResult, DomainUserEditRequest userEditRequest) {
    if (userEditRequest.isRemoveAvatar()) {
      domainUserService.removeUserAvatar(userEditRequest.getSamAccountName());
    } else if (!isEmpty(userEditRequest.getAvatar()) && !userEditRequest.getAvatar().isEmpty()) {
      MultipartFile file = userEditRequest.getAvatar();
      try (InputStream in = file.getInputStream()) {
        domainUserService.updateUserAvatar(userEditRequest.getSamAccountName(), in);
      } catch (IOException e) {
        getLogger().error("updateAvatar({})", userEditRequest, e);
        bindingResult.rejectValue("avatar", "todo", "Uploading avatar failed.");
      }
    }
  }

  private void handleException(BindingResult bindingResult, ServiceException serviceException) {

    Object bindTarget = bindingResult.getTarget();
    getLogger().debug("handleException of bind target '{}'", bindTarget, serviceException);
    Assert.isTrue(bindTarget instanceof DomainUserEditRequest, "Illegal bind target.");
    String errorCode = requireNonNullElse(serviceException.getErrorCode(), "");
    switch (errorCode) {
      case EC_SAM_ACCOUNT_NAME_REQUIRED: {
        bindingResult.rejectValue("samAccountName", "code",
            "Username is required.");
        break;
      }
      case EC_SAM_ACCOUNT_ALREADY_EXISTS: {
        bindingResult.rejectValue("samAccountName", "code",
            "Username already exists.");
        break;
      }
      case EC_ILLEGAL_SAM_ACCOUNT_NAME: {
        bindingResult.rejectValue("samAccountName", "code",
            "Username contains illegal characters.");
        break;
      }
      case EC_ILLEGAL_FIRST_NAME: {
        bindingResult.rejectValue("firstName", "code",
            "First name contains illegal characters.");
        break;
      }
      case EC_ILLEGAL_LAST_NAME: {
        bindingResult.rejectValue("lastName", "code",
            "Last name contains illegal characters.");
        break;
      }
      case EC_PRINCIPAL_ALREADY_EXISTS: {
        bindingResult.rejectValue("userPrincipalName", "code",
            "User principal name already exists.");
        break;
      }
      case EC_UID_ALREADY_EXISTS: {
        bindingResult.rejectValue("uid", "code",
            "User's unix uid already exists.");
        break;
      }
      case EC_UID_NUMBER_ALREADY_EXISTS: {
        bindingResult.rejectValue("uidNumber", "code",
            "User's unix uid number already exists.");
        break;
      }
      case EC_DN_ALREADY_EXISTS: {
        bindingResult.rejectValue("samAccountName", "code",
            "Distinguished name already exists.");
        break;
      }
      case EC_EMPTY_OU_RDN: {
        bindingResult.rejectValue("newOu", "code",
            "Organizational unit is empty.");
        break;
      }
      case EC_OU_NOT_FOUND: {
        bindingResult.rejectValue("newOu", "code",
            "Organizational unit was not found.");
        break;
      }
      default: {
        getLogger().error("Editing user failed with a not mapped exception.", serviceException);
        throw serviceException;
      }
    }
  }
}
