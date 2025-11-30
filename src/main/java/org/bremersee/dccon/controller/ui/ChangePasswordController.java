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

package org.bremersee.dccon.controller.ui;

import static java.util.Objects.requireNonNullElse;
import static org.springframework.util.ObjectUtils.isEmpty;

import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.model.ChangePasswordRequest;
import org.bremersee.dccon.service.DomainService;
import org.bremersee.dccon.service.DomainUserService;
import org.bremersee.exception.ServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;

/**
 * The type ChangePasswordController.
 *
 * @author Christian Bremer
 */
@Controller
public class ChangePasswordController extends AbstractController {

  private final DomainService domainService;

  private final DomainUserService domainUserService;

  public ChangePasswordController(
      DomainControllerProperties domainControllerProperties,
      LocaleResolver localeResolver,
      DomainService domainService,
      DomainUserService domainUserService) {
    super(domainControllerProperties, localeResolver);
    this.domainService = domainService;
    this.domainUserService = domainUserService;
  }

  @ModelAttribute("passwordPattern")
  public String getPasswordPattern() {
    return domainService.getPasswordInformation().getPasswordRegex();
  }

  @GetMapping(path = "/change-password")
  public String displayChangePassword(
      @RequestParam(value = "username", required = false) String username,
      ModelMap model) {

    ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!isEmpty(authentication) && authentication.isAuthenticated()) {
      changePasswordRequest.setUsername(authentication.getName());
    } else if (!isEmpty(username)) {
      changePasswordRequest.setUsername(username);
    }
    model.addAttribute("changePasswordRequest", changePasswordRequest);
    return "change-password";
  }

  @PostMapping(path = "change-password")
  public String changePassword(
      @ModelAttribute(name = "changePasswordRequest") ChangePasswordRequest changePasswordRequest,
      BindingResult bindingResult) {

    String username = changePasswordRequest.getUsername();
    String oldPassword = changePasswordRequest.getOldPassword();
    String newPassword = requireNonNullElse(changePasswordRequest.getNewPassword(), "");
    String newPasswordRepetition = changePasswordRequest.getNewPasswordRepetition();
    if (!newPassword.equals(newPasswordRepetition)) {
      bindingResult.rejectValue("newPasswordRepetition", "todo", "Passwords must be equal.");
      return "change-password";
    }
    try {
      domainUserService.updateUserPassword(username, oldPassword, newPassword);

    } catch (AuthenticationException ae) {
      bindingResult.rejectValue("oldPassword", "todo", "Authentication failed.");
      return "change-password";

    } catch (ServiceException se) {
      if (EC_PASSWORD_RESTRICTIONS.equals(se.getErrorCode())) {
        bindingResult.rejectValue("newPassword", "todo",
            "Password doesn't match the required pattern.");
        return "change-password";
      }
      throw se;
    }

    return "redirect:password-changed";
  }

  @GetMapping(path = "/password-changed")
  public String displayPasswordChanged() {
    return "password-changed";
  }
}
