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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.Getter;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.AbstractController;
import org.bremersee.dccon.controller.ui.components.FieldTemplateComponent;
import org.bremersee.dccon.controller.ui.components.PageableComponent;
import org.bremersee.dccon.controller.ui.components.RedirectComponent;
import org.bremersee.dccon.controller.ui.model.DomainUserAddRequest;
import org.bremersee.dccon.controller.ui.model.RedirectMessage;
import org.bremersee.dccon.controller.ui.model.RedirectMessageType;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.model.OrganizationalUnit;
import org.bremersee.dccon.model.SelectOption;
import org.bremersee.dccon.service.DomainService;
import org.bremersee.dccon.service.DomainUserService;
import org.bremersee.dccon.service.OrganizationalUnitService;
import org.bremersee.dccon.service.TemplateEngine;
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
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The type UsersController.
 *
 * @author Christian Bremer
 */
@Controller
public class UserAddController extends AbstractController
    implements PageableComponent, RedirectComponent, FieldTemplateComponent {

  private final DomainService domainService;

  private final DomainUserService domainUserService;

  private final OrganizationalUnitService organizationalUnitService;

  @Getter
  private final TemplateEngine templateEngine;

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
  }

  @ModelAttribute("passwordPattern")
  public String getPasswordPattern() {
    return domainService.getPasswordInformation().getPasswordRegex();
  }

  @ModelAttribute("rfc2307Enabled")
  public boolean isRfc2307Enabled() {
    return domainService.isRfc2307Enabled();
  }

  @ModelAttribute(SCOPE)
  public SearchScope getSearchScope(
      @RequestParam(name = SCOPE, required = false) SearchScope scope) {
    return scope;
  }

  @ModelAttribute("ous")
  public List<SelectOption<OrganizationalUnit>> getOrganizationalUnits(
      @RequestParam(name = OU, required = false) Dn ou,
      ModelMap model) {

    Dn ouDn = Optional.ofNullable(model.get("userAddRequest"))
        .map(obj -> obj instanceof DomainUserAddRequest)
        .map(DomainUserAddRequest.class::cast)
        .map(DomainUserAddRequest::getOuDn)
        .filter(dn -> !dn.isSame(getProperties().getBaseDn()))
        .orElseGet(() -> Optional.ofNullable(ou)
            .filter(dn -> !dn.isEmpty())
            .filter(dn -> !dn.isSame(getProperties().getBaseDn()))
            .orElseGet(() -> getProperties().getUser().getDefaultUserOu()));
    return organizationalUnitService.getOrganizationalUnitSelectors(ouDn);
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
        .orElseGet(() -> getProperties().getBaseDn(getProperties().getUser().getDefaultUserOu()));
    DomainUserAddRequest userAddRequest = new DomainUserAddRequest();
    userAddRequest.setUser(createNewDomainUser());
    userAddRequest.setOu(ouDn.format());
    userAddRequest.setUseUsernameAsCn(getProperties().getUser().isUseUsernameAsCn());
    userAddRequest.setSendEmail(false);
    model.addAttribute("userAddRequest", userAddRequest);
    return "admin/user-add";
  }

  @PostMapping(path = "/admin/user-add")
  public String addUser(
      @ModelAttribute(name = PAGE, binding = false) Integer page,
      @ModelAttribute(name = SIZE, binding = false) Integer size,
      @ModelAttribute(name = SORT, binding = false) String sort,
      @ModelAttribute(name = QUERY, binding = false) String query,
      @ModelAttribute(name = SCOPE, binding = false) SearchScope scope,
      @ModelAttribute(name = "userAddRequest") DomainUserAddRequest userAddRequest,
      ModelMap model,
      HttpServletRequest request,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {

    getLogger().debug("addUser({}, {}, {}, {}, {}, {})",
        userAddRequest, page, size, sort, query, scope);
    Map<String, Object> parameters = Map.of(
        PAGE, Optional.ofNullable(page).orElse(PAGE_DEFAULT_INT),
        SIZE, Optional.ofNullable(size)
            .filter(s -> s > 0)
            .orElse(SIZE_DEFAULT_INT),
        SORT, Optional.ofNullable(sort).orElse(USER_SORT),
        QUERY, Optional.ofNullable(query).orElse(""),
        OU, Optional.ofNullable(userAddRequest)
            .map(DomainUserAddRequest::getOuDn)
            .map(Dn::format)
            .orElse(getProperties().getUser().getDefaultUserOu().format()),
        SCOPE, Optional.ofNullable(scope).orElse(SearchScope.ONELEVEL)
    );

    getLogger().debug("Try to add user '{}'.", userAddRequest);

    if (isEmpty(userAddRequest)) {
      String redirect = getRedirectUri("/admin/users", PAGE_AND_OU_PARAMS, parameters);
      getLogger().debug("User add request is empty. Redirecting to {}", redirect);
      return redirect;
    }

    processTemplates(bindingResult, userAddRequest.getUser());
    DomainUser addedUser = addUser(
        bindingResult,
        userAddRequest.getUser(),
        Optional.ofNullable(userAddRequest.getOu())
            .map(Dn::new)
            .orElseGet(() -> getProperties().getUser().getDefaultUserOu()),
        userAddRequest.getUseUsernameAsCn(),
        userAddRequest.getSendEmail());

    if (bindingResult.hasErrors()) {
      getLogger().debug("Adding user failed. Some fields were invalid.");
      return "admin/user-add";
    }

    model.clear();
    String msg = getMessageSource().getMessage(
        "i18n.user.added",
        new Object[]{addedUser.getDisplayName()},
        String.format("User '%s' was successfully added.", addedUser.getDisplayName()),
        resolveLocale(request));
    RedirectMessage rmsg = new RedirectMessage(msg, RedirectMessageType.SUCCESS);
    redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);

    parameters = new HashMap<>(parameters);
    parameters.put("user", addedUser);
    String redirect = getRedirectUri("user-edit?user={{user.samAccountName}}",
        PAGE_AND_OU_PARAMS, parameters);
    getLogger().debug("User successfully added. Redirecting to {}", redirect);
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
      case EC_SAM_ACCOUNT_ALREADY_EXISTS: {
        bindingResult.rejectValue("user.samAccountName", "code",
            "Username already exists.");
        getProperties().getUser().replaceInvalidUsernameWithDefaults(user, isRfc2307Enabled());
        break;
      }
      case EC_PASSWORD_RESTRICTIONS: {
        bindingResult.rejectValue("user.password", "code",
            "Password restrictions are not met.");
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
