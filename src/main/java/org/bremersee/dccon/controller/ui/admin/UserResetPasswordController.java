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
import java.util.Optional;
import java.util.regex.Pattern;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.components.OrganizationalUnitComponent;
import org.bremersee.dccon.controller.ui.components.PageableComponent;
import org.bremersee.dccon.controller.ui.model.DomainUserResetPasswordRequest;
import org.bremersee.dccon.controller.ui.model.RedirectMessage;
import org.bremersee.dccon.controller.ui.model.RedirectMessageType;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.model.TreeSearchScope;
import org.bremersee.dccon.service.DomainService;
import org.bremersee.dccon.service.DomainUserService;
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
public class UserResetPasswordController extends AbstractEditController implements
    PageableComponent,
    OrganizationalUnitComponent {

  private final DomainService domainService;

  private final DomainUserService domainUserService;

  public UserResetPasswordController(
      DomainControllerProperties domainControllerProperties,
      LocaleResolver localeResolver,
      DomainService domainService,
      DomainUserService domainUserService) {
    super(domainControllerProperties, localeResolver);
    this.domainUserService = domainUserService;
    this.domainService = domainService;
  }

  @Override
  public String getDefaultSort() {
    return USER_SORT;
  }

  @ModelAttribute("passwordPattern")
  public String getPasswordPattern() {
    return domainService.getPasswordInformation().getPasswordRegex();
  }

  @GetMapping(path = "/admin/user-reset-password")
  public String displayUserResetPassword(
      @RequestParam(value = "user", required = false) String userName,
      @RequestParam(value = OU, required = false) Dn ou,
      @RequestParam(value = SCOPE, required = false) TreeSearchScope searchScope,
      ModelMap model,
      RedirectAttributes redirectAttributes) {

    return Optional.ofNullable(userName)
        .flatMap(name -> domainUserService.getUser(userName, ou, searchScope))
        .map(user -> {
          model.addAttribute("user", user);
          model.addAttribute("passwordRequest", new DomainUserResetPasswordRequest(user));
          return "admin/user-reset-password";
        })
        .orElseGet(() -> entityNotFoundRedirect(
            redirectAttributes, "User", "todo", userName, "users"));
  }

  @PostMapping(path = "/admin/user-reset-password")
  public String resetPassword(
      @RequestParam(value = OU, required = false) Dn ou,
      @RequestParam(value = SCOPE, required = false) TreeSearchScope searchScope,
      @ModelAttribute("passwordRequest") DomainUserResetPasswordRequest passwordRequest,
      ModelMap model,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {

    getLogger().debug("resetPassword({})", passwordRequest);
    return Optional.ofNullable(passwordRequest.getSamAccountName())
        .flatMap(name -> domainUserService.getUser(name, ou, searchScope))
        .map(user -> resetPassword(
            user, passwordRequest, model, bindingResult, redirectAttributes))
        .orElseGet(() -> entityNotFoundRedirect(
            redirectAttributes, "User", "todo", passwordRequest.getSamAccountName(),
            "users"));
  }

  private String resetPassword(
      DomainUser user,
      DomainUserResetPasswordRequest passwordRequest,
      ModelMap model,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {

    String password;
    if (passwordRequest.isGenerateRandomPassword()) {
      password = domainService.createRandomPassword();
    } else if (!isEmpty(passwordRequest.getPassword())) {
      password = passwordRequest.getPassword();
      if (!Pattern.compile(getPasswordPattern()).matcher(password).matches()) {
        String defaultMsg = "Password doesn't match the required pattern.";
        bindingResult.rejectValue("password", "todo", defaultMsg);
        model.addAttribute("user", user);
        return "admin/user-reset-password";
      }
    } else {
      String defaultMsg = "Resetting password failed. Password is required.";
      bindingResult.rejectValue("password", "todo", defaultMsg);
      model.addAttribute("user", user);
      return "admin/user-reset-password";
    }
    domainUserService.updateUserPassword(user.getSamAccountName(), password,
        passwordRequest.isGenerateRandomPassword());

    String defaultMsg = "Password was successfully changed.";
    RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.SUCCESS, defaultMsg, "todo");
    redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);
    Map<String, Object> parameters = getParamterMap();
    String redirect = getRedirectUri("user-reset-password?user={{userName}}",
        PAGE_AND_OU_PARAMS, putToParameterMap(parameters, "userName", user.getSamAccountName()));
    logRedirectTo(defaultMsg, redirect);
    return redirect;
  }

}
