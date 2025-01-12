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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.Getter;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.AbstractController;
import org.bremersee.dccon.controller.ui.components.FieldTemplateComponent;
import org.bremersee.dccon.controller.ui.components.OrganisationalUnitsComponent;
import org.bremersee.dccon.controller.ui.components.OrganizationalUnitComponent;
import org.bremersee.dccon.controller.ui.components.PageableComponent;
import org.bremersee.dccon.controller.ui.components.RedirectComponent;
import org.bremersee.dccon.controller.ui.model.DomainUserAddRequest;
import org.bremersee.dccon.controller.ui.model.RedirectMessage;
import org.bremersee.dccon.controller.ui.model.RedirectMessageType;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.service.DomainService;
import org.bremersee.dccon.service.DomainUserService;
import org.bremersee.dccon.service.OrganizationalUnitService;
import org.bremersee.dccon.service.TemplateEngine;
import org.bremersee.exception.ServiceException;
import org.ldaptive.dn.Dn;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The type UsersController.
 *
 * @author Christian Bremer
 */
@Controller
public class UserAddController extends AbstractController
    implements PageableComponent, RedirectComponent, FieldTemplateComponent,
    OrganizationalUnitComponent, OrganisationalUnitsComponent {

  private final DomainService domainService;

  private final DomainUserService domainUserService;

  @Getter
  private final OrganizationalUnitService organizationalUnitService;

  @Getter
  private final TemplateEngine templateEngine;

  private final Pattern emailPattern;

  public UserAddController(
      DomainControllerProperties domainControllerProperties,
      LocaleResolver localeResolver,
      DomainService domainService,
      DomainUserService domainUserService,
      OrganizationalUnitService organizationalUnitService,
      TemplateEngine templateEngine) {
    super(domainControllerProperties, localeResolver);
    this.domainService = domainService;
    this.domainUserService = domainUserService;
    this.organizationalUnitService = organizationalUnitService;
    this.templateEngine = templateEngine;
    this.emailPattern = Pattern.compile(domainControllerProperties.getEmailRegex());
  }

  @ModelAttribute("passwordPattern")
  public String getPasswordPattern() {
    return domainService.getPasswordInformation().getPasswordRegex();
  }

  @ModelAttribute("rfc2307Enabled")
  public boolean isRfc2307Enabled() {
    return domainService.isRfc2307Enabled();
  }

  @Override
  public String getDefaultSort() {
    return USER_SORT;
  }

  @GetMapping(path = "/admin/user-add")
  public String displayUserAdd(
      @RequestParam(name = OU, required = false) Dn ou,
      ModelMap model) {

    getLogger().debug("displayUserAdd({})", ou);
    Dn ouDn = Optional.ofNullable(ou)
        .filter(dn -> !dn.isEmpty())
        .filter(dn -> !dn.isSame(getProperties().getBaseDn()))
        .orElseGet(() -> getProperties().getBaseDn(getProperties().getUser().getDefaultOu()));
    DomainUserAddRequest userAddRequest = new DomainUserAddRequest();
    userAddRequest.setUser(createNewDomainUser());
    userAddRequest.setNewOu(ouDn.format());
    userAddRequest.setUseUsernameAsCn(getProperties().getUser().isUseUsernameAsCn());
    userAddRequest.setSendEmail(false);
    model.addAttribute("userAddRequest", userAddRequest);
    return "admin/user-add";
  }

  @PostMapping(path = "/admin/user-add")
  public String addUser(
      @ModelAttribute(name = "userAddRequest") DomainUserAddRequest userAddRequest,
      ModelMap model,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {

    getLogger().debug("addUser({})", userAddRequest);

    processTemplates(bindingResult, userAddRequest.getUser());

    if (!isEmpty(userAddRequest.getUser().getEmail())
        && !emailPattern.matcher(userAddRequest.getUser().getEmail()).matches()) {
      bindingResult.rejectValue("user.email", "code",
          "Email is invalid.");
    }
    if (userAddRequest.isSendEmail() && isEmpty(userAddRequest.getUser().getEmail())) {
      bindingResult.rejectValue("user.email", "code",
          "If you want to send an invitation email, you have to enter an email address.");
    }
    if (userAddRequest.isGenerateRandomPassword()) {
      userAddRequest.getUser().setPassword(null);
    } else if (isEmpty(userAddRequest.getUser().getPassword())) {
      bindingResult.rejectValue("user.password", "code",
          "Password is required.");
    }
    if (bindingResult.hasErrors()) {
      getLogger().debug("Adding user failed. Some fields were invalid.");
      return "admin/user-add";
    }
    DomainUser addedUser = addUser(
        bindingResult,
        userAddRequest.getUser(),
        Optional.ofNullable(userAddRequest.getNewOu())
            .map(Dn::new)
            .orElseGet(() -> getProperties().getUser().getDefaultOu()),
        userAddRequest.isUseUsernameAsCn(),
        userAddRequest.isSendEmail());

    if (bindingResult.hasErrors()) {
      getLogger().debug("Adding user failed. Some fields were invalid.");
      return "admin/user-add";
    }

    model.clear();
    String msg = String.format("User '%s' was successfully added.", addedUser.getName());
    RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.SUCCESS, msg,
        "i18n.user.added", addedUser.getName());
    redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);

    Map<String, Object> parameters = getParamterMap(userAddRequest.getNewOuDn());
    String redirect = getRedirectUri("user-edit?user={{user.samAccountName}}",
        PAGE_AND_OU_PARAMS, putToParameterMap(parameters, "user", addedUser));
    logRedirectTo("User successfully added.", redirect);
    return redirect;
  }

  private DomainUser addUser(BindingResult bindingResult, DomainUser user, Dn ou,
      boolean useUsernameAsCn, boolean sendEmail) {
    try {
      return domainUserService.addUser(user, ou, useUsernameAsCn, sendEmail);

    } catch (ServiceException e) {
      handleException(bindingResult, e);
    }
    return user;
  }

  private void handleException(BindingResult bindingResult, ServiceException serviceException) {

    Object bindTarget = bindingResult.getTarget();
    getLogger().debug("handleException of bind target '{}'", bindTarget, serviceException);

    if (!(bindTarget instanceof DomainUserAddRequest)) {
      return;
    }
    DomainUser user = ((DomainUserAddRequest) bindTarget).getUser();

    String errorCode = Objects.requireNonNullElse(serviceException.getErrorCode(), "");
    switch (errorCode) {
      case EC_SAM_ACCOUNT_NAME_REQUIRED: {
        bindingResult.rejectValue("user.samAccountName", "code",
            "Username is required.");
        break;
      }
      case EC_ILLEGAL_SAM_ACCOUNT_NAME: {
        bindingResult.rejectValue("user.samAccountName", "code",
            "Username contains illegal characters.");
        break;
      }
      case EC_ILLEGAL_FIRST_NAME: {
        bindingResult.rejectValue("user.firstName", "code",
            "First name contains illegal characters.");
        break;
      }
      case EC_ILLEGAL_LAST_NAME: {
        bindingResult.rejectValue("user.lastName", "code",
            "Last name contains illegal characters.");
        break;
      }
      case EC_SAM_ACCOUNT_ALREADY_EXISTS: {
        bindingResult.rejectValue("user.samAccountName", "code",
            "Username already exists.");
        getProperties().getUser().replaceInvalidUsernameWithDefaults(user, isRfc2307Enabled());
        break;
      }
      case EC_UID_ALREADY_EXISTS: {
        bindingResult.rejectValue("user.uid", "code",
            "User's unix uid already exists.");
        break;
      }
      case EC_UID_NUMBER_ALREADY_EXISTS: {
        bindingResult.rejectValue("user.uidNumber", "code",
            "User's unix uid number already exists.");
        break;
      }
      case EC_PASSWORD_RESTRICTIONS: {
        bindingResult.rejectValue("user.password", "code",
            "Password restrictions are not met.");
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
      case EC_ADDING_USER_FAILED: { // TODO global
        getLogger().error("Adding user failed.", serviceException);
        bindingResult.rejectValue("user.samAccountName", "code",
            "Something went wrong. Please try again later.");
        break;
      }
      default: {
        getLogger().error("Adding user failed with a not mapped exception.", serviceException);
        throw serviceException;
      }
    }
  }

  private DomainUser createNewDomainUser() {
    DomainUser domainUser = new DomainUser();
    getProperties().getUser().fillDefaults(domainUser, isRfc2307Enabled());
    return domainUser;
  }

  private void processTemplates(BindingResult bindingResult, DomainUser user) {
    Map<String, Object> map = Map.of("user", user);

    String value = processTemplatedField(bindingResult, "company", user.getCompany(), map);
    user.setCompany(value);

    value = processTemplatedField(bindingResult, "department", user.getDepartment(), map);
    user.setDepartment(value);

    value = processTemplatedField(bindingResult, "description", user.getDescription(), map);
    user.setDescription(value);

    value = processTemplatedField(bindingResult, "displayName", user.getDisplayName(), map);
    user.setDisplayName(value);

    value = processTemplatedField(bindingResult, "email", user.getEmail(), map);
    user.setEmail(value);

    value = processTemplatedField(bindingResult, "gecos", user.getGecos(), map);
    user.setGecos(value);

    value = processTemplatedField(bindingResult, "homeDirectory", user.getHomeDirectory(), map);
    user.setHomeDirectory(value);

    value = processTemplatedField(bindingResult, "loginShell", user.getLoginShell(), map);
    user.setLoginShell(value);

    value = processTemplatedField(bindingResult, "nisDomain", user.getNisDomain(), map);
    user.setNisDomain(value);

    value = processTemplatedField(bindingResult, "physicalDeliveryOfficeName",
        user.getPhysicalDeliveryOfficeName(), map);
    user.setPhysicalDeliveryOfficeName(value);

    value = processTemplatedField(bindingResult, "preferredLanguage", user.getPreferredLanguage(),
        map);
    user.setPreferredLanguage(value);

    value = processTemplatedField(bindingResult, "profilePath", user.getProfilePath(), map);
    user.setProfilePath(value);

    value = processTemplatedField(bindingResult, "scriptPath", user.getScriptPath(), map);
    user.setScriptPath(value);

    value = processTemplatedField(bindingResult, "uid", user.getUid(), map);
    user.setUid(value);

    value = processTemplatedField(bindingResult, "unixHomeDirectory", user.getUnixHomeDirectory(),
        map);
    user.setUnixHomeDirectory(value);
  }

}
