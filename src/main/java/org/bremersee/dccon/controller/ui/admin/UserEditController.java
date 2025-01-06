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

import static org.springframework.util.ObjectUtils.isEmpty;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.AbstractController;
import org.bremersee.dccon.controller.ui.components.PageableComponent;
import org.bremersee.dccon.controller.ui.components.RedirectComponent;
import org.bremersee.dccon.controller.ui.model.DomainUserEditRequest;
import org.bremersee.dccon.controller.ui.model.RedirectMessage;
import org.bremersee.dccon.controller.ui.model.RedirectMessageType;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.service.DomainService;
import org.bremersee.dccon.service.DomainUserService;
import org.bremersee.dccon.service.OrganizationalUnitService;
import org.bremersee.exception.ServiceException;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
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
public class UserEditController extends AbstractController
    implements PageableComponent, RedirectComponent {

  private final DomainService domainService;

  private final DomainUserService domainUserService;

  private final OrganizationalUnitService organizationalUnitService;

  public UserEditController(
      DomainControllerProperties domainControllerProperties,
      LocaleResolver localeResolver,
      DomainService domainService,
      DomainUserService domainUserService,
      OrganizationalUnitService organizationalUnitService) {
    super(domainControllerProperties, localeResolver);
    this.domainService = domainService;
    this.domainUserService = domainUserService;
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

  @ModelAttribute(SCOPE)
  public SearchScope getSearchScope(
      @RequestParam(name = SCOPE, required = false) SearchScope scope) {
    return scope;
  }

  @GetMapping(path = "/admin/user-edit")
  public String displayUserEdit(
      @RequestParam(value = "user", required = false) String userName,
      ModelMap model) {

    return Optional.ofNullable(userName)
        .flatMap(name -> domainUserService.getUser(name, null, null))
        .map(user -> {
          DomainUserEditRequest req = new DomainUserEditRequest(
              user, getProperties().getParentDn(user.getDistinguishedName()));
          model.addAttribute("userEditRequest", req);
          model.addAttribute("ou", req.getOu());
          return req;
        })
        .map(req -> {
          model.addAttribute("ous", organizationalUnitService
              .getOrganizationalUnitSelectors(req.getOuDn()));
          return "admin/user-edit";
        })
        .orElse("admin/user-add");
  }

  @PostMapping(path = "/admin/user-edit")
  public String updateUser(
      @ModelAttribute(name = PAGE, binding = false) Integer page,
      @ModelAttribute(name = SIZE, binding = false) Integer size,
      @ModelAttribute(name = SORT, binding = false) String sort,
      @ModelAttribute(name = QUERY, binding = false) String query,
      @ModelAttribute(name = SCOPE, binding = false) SearchScope scope,
      @ModelAttribute(name = "userEditRequest") DomainUserEditRequest userEditRequest,
      ModelMap model,
      HttpServletRequest request,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) throws IOException {

    getLogger().debug("addUser({}, {}, {}, {}, {}, {})",
        userEditRequest, page, size, sort, query, scope);
    Map<String, Object> parameters = Map.of(
        PAGE, Optional.ofNullable(page).orElse(PAGE_DEFAULT_INT),
        SIZE, Optional.ofNullable(size)
            .filter(s -> s > 0)
            .orElse(SIZE_DEFAULT_INT),
        SORT, Optional.ofNullable(sort).orElse(USER_SORT),
        QUERY, Optional.ofNullable(query).orElse(""),
        OU, Optional.ofNullable(userEditRequest)
            .map(DomainUserEditRequest::getOuDn)
            .map(Dn::format)
            .or(() -> Optional.ofNullable(userEditRequest)
                .map(DomainUserEditRequest::getUser)
                .map(DomainUser::getDistinguishedName)
                .map(dn -> getProperties().getParentDn(dn))
                .map(Dn::format))
            .orElse(getProperties().getUser().getDefaultUserOu().format()),
        SCOPE, Optional.ofNullable(scope).orElse(SearchScope.ONELEVEL)
    );

    getLogger().debug("Try to edit user '{}'.", userEditRequest);

    if (isEmpty(userEditRequest)) {
      String redirect = getRedirectUri("/admin/users", PAGE_AND_OU_PARAMS, parameters);
      getLogger().debug("User edit request is empty. Redirecting to {}", redirect);
      return redirect;
    }

    processNameChanges(userEditRequest);
    DomainUser updatedUser = updateUser(bindingResult, userEditRequest);
    updateAvatar(bindingResult, userEditRequest);

    model.clear();
    String msg = getMessageSource().getMessage(
        "i18n.user.edited",
        new Object[]{updatedUser.getDisplayName()}, //  TODO
        String.format("User '%s' was successfully added.", updatedUser.getDisplayName()), //  TODO
        resolveLocale(request));
    RedirectMessage rmsg = new RedirectMessage(msg, RedirectMessageType.SUCCESS);
    redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);

    parameters = new HashMap<>(parameters);
    parameters.put("user", updatedUser);
    String redirect = getRedirectUri("user-edit?user={{user.samAccountName}}",
        PAGE_AND_OU_PARAMS, parameters);
    getLogger().debug("User successfully updated. Redirecting to {}", redirect);
    return redirect;
  }

  private void processNameChanges(DomainUserEditRequest userEditRequest) {
    DomainUser user = userEditRequest.getUser();
    if (userEditRequest.isRenameSamAccountNameAutomatically()) {
      getProperties().getUser().replaceNames(
          user, userEditRequest.getOldSamAccountName(), user.getSamAccountName());
    }
    if (userEditRequest.isRenameFirstNameAutomatically()) {
      getProperties().getUser().replaceNames(
          user, userEditRequest.getOldFirstName(), user.getFirstName());
    }
    if (userEditRequest.isRenameLastNameAutomatically()) {
      getProperties().getUser().replaceNames(
          user, userEditRequest.getOldLastName(), user.getLastName());
    }
    // TODO check displayName and gecos
  }

  private DomainUser updateUser(BindingResult bindingResult,
      DomainUserEditRequest userEditRequest) {
    String userName = userEditRequest.getOldSamAccountName();
    DomainUser user = userEditRequest.getUser();
    Dn ou = userEditRequest.getOuDn();
    try {
      Dn parentDn = getProperties().getParentDn(user.getDistinguishedName());
      Dn ouDn = getProperties().getBaseDn(ou);
      Dn newOu = parentDn.isSame(ouDn) ? null : ouDn;
      return domainUserService.updateUser(userName, user, newOu);

    } catch (ServiceException e) {
      handleException(bindingResult, e);
    }
    return user;
  }

  private void updateAvatar(BindingResult bindingResult, DomainUserEditRequest userEditRequest)
      throws IOException {
    try {
      if (userEditRequest.isRemoveAvatar()) {
        domainUserService.removeUserAvatar(userEditRequest.getUser().getSamAccountName());
      } else if (!isEmpty(userEditRequest.getAvatar()) && !userEditRequest.getAvatar().isEmpty()) {
        MultipartFile file = userEditRequest.getAvatar();
        try (InputStream in = file.getInputStream()) {
          domainUserService.updateUserAvatar(userEditRequest.getUser().getSamAccountName(), in);
        }
      }

    } catch (ServiceException e) {
      handleException(bindingResult, e);
    }
  }

  private void handleException(BindingResult bindingResult, ServiceException serviceException) {

    getLogger().debug("handleException of bind target '{}'",
        bindingResult.getTarget(), serviceException);

    if (!(bindingResult.getTarget() instanceof DomainUserEditRequest)) {
      return;
    }
    DomainUser user = ((DomainUserEditRequest) bindingResult.getTarget()).getUser();

    String errorCode = Objects.requireNonNullElse(serviceException.getErrorCode(), "");
    switch (errorCode) {
      case EC_SAM_ACCOUNT_NAME_REQUIRED: {
        bindingResult.rejectValue("user.samAccountName", "code",
            "Username is required.");
        break;
      }
      case EC_SAM_ACCOUNT_ALREADY_EXISTS: {
        bindingResult.rejectValue("user.samAccountName", "code",
            "Username already exists.");
        break;
      }
      case EC_DN_ALREADY_EXISTS: {
        bindingResult.rejectValue("user.samAccountName", "code",
            "Distinguished name already exists.");
        break;
      }
      case EC_EMPTY_OU_RDN: {
        bindingResult.rejectValue("ou", "code",
            "Organizational unit is empty.");
        break;
      }
      case EC_OU_NOT_FOUND: {
        bindingResult.rejectValue("ou", "code",
            "Organizational unit was not found.");
        break;
      }
      case EC_UPDATING_USER_FAILED: { // TODO global
        getLogger().error("Editing user failed.", serviceException);
        bindingResult.rejectValue("user.samAccountName", "code",
            "Something went wrong. Please try again later.");
        break;
      }
      default: {
        getLogger().error("Editing user failed with a not mapped exception.", serviceException);
        throw serviceException;
      }
    }
  }
}
