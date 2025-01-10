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
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.AbstractController;
import org.bremersee.dccon.controller.ui.components.OrganizationalUnitComponent;
import org.bremersee.dccon.controller.ui.components.PageableComponent;
import org.bremersee.dccon.controller.ui.components.RedirectComponent;
import org.bremersee.dccon.controller.ui.model.RedirectMessage;
import org.bremersee.dccon.controller.ui.model.RedirectMessageType;
import org.bremersee.dccon.service.DomainService;
import org.bremersee.dccon.service.DomainUserService;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
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
public class UserEditMiscController extends AbstractController implements PageableComponent,
    RedirectComponent, OrganizationalUnitComponent {

  private final DomainService domainService;

  private final DomainUserService domainUserService;

  public UserEditMiscController(
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

  @GetMapping(path = "/admin/user-edit-misc")
  public String displayUserEditDangerZone(
      @RequestParam(value = "user", required = false) String userName,
      @RequestParam(value = OU, required = false) Dn ou,
      @RequestParam(value = SCOPE, required = false) SearchScope searchScope,
      ModelMap model) {

    return Optional.ofNullable(userName)
        .flatMap(name -> domainUserService.getUser(userName, ou, searchScope))
        .map(user -> {
          model.addAttribute("user", user);
          return "admin/user-edit-misc";
        })
        .orElseGet(() -> {
          String msg = String.format("User '%s' not found.", userName);
          model.addAttribute("rmsg", new RedirectMessage(msg, RedirectMessageType.WARNING));
          return "admin/user-edit-misc";
        });
  }

  @PostMapping(path = "/admin/user-edit-misc-reset-password")
  public String resetPassword(
      @RequestParam(value = PAGE, required = false) Integer page,
      @RequestParam(value = SIZE, required = false) Integer size,
      @RequestParam(value = SORT, required = false) String sort,
      @RequestParam(value = QUERY, required = false) String query,
      @RequestParam(value = OU, required = false) Dn ou,
      @RequestParam(value = SCOPE, required = false) SearchScope scope,
      @RequestParam(value = "user", required = false) String userName,
      @RequestParam(value = "password", required = false) String newPassword,
      @RequestParam(value = "generateRandomPassword", defaultValue = "false") boolean generateRandomPassword,
      @RequestParam(value = "sendEmail", defaultValue = "false") boolean sendEmail,
      RedirectAttributes redirectAttributes) {

    getLogger().debug("resetPassword({}, {}, {}, {}, {}, {}, {}, {}, {})",
        page, size, sort, query, scope, userName, "****", generateRandomPassword, sendEmail);
    Map<String, Object> parameters = getParamterMap(page, size, sort, query, ou, scope);

    if (isEmpty(userName)) {
      String defaultMsg = "Resetting password failed. Username is required.";
      RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.DANGER, defaultMsg, "todo");
      redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);
      String redirect = getRedirectUri("users", PAGE_AND_OU_PARAMS, parameters);
      logRedirectTo(defaultMsg, redirect);
      return redirect;
    }

    Map<String, Object> parametersWithUsername = addToMap(parameters, "userName", userName);

    String password;
    if (generateRandomPassword) {
      password = domainService.createRandomPassword();
    } else if (isEmpty(newPassword)) {
      String defaultMsg = "Resetting password failed. Password is required.";
      RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.DANGER, defaultMsg, "todo");
      redirectAttributes.addFlashAttribute("pmsg", rmsg);
      String redirect = getRedirectUri("user-edit-misc?user={{userName}}",
          PAGE_AND_OU_PARAMS, parametersWithUsername);
      logRedirectTo(defaultMsg, redirect);
      return redirect;
    } else {
      password = newPassword;
    }

    domainUserService.updateUserPassword(userName, password, sendEmail);

    String defaultMsg = "Password was successfully changed.";
    RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.SUCCESS, defaultMsg, "todo");
    redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);
    String redirect = getRedirectUri("user-edit-misc?user={{userName}}",
        PAGE_AND_OU_PARAMS, parametersWithUsername);
    logRedirectTo(defaultMsg, redirect);
    return redirect;
  }

  @PostMapping(path = "/admin/user-edit-misc-delete-user")
  public String deleteUser(
      @RequestParam(value = PAGE, required = false) Integer page,
      @RequestParam(value = SIZE, required = false) Integer size,
      @RequestParam(value = SORT, required = false) String sort,
      @RequestParam(value = QUERY, required = false) String query,
      @RequestParam(value = OU, required = false) Dn ou,
      @RequestParam(value = SCOPE, required = false) SearchScope scope,
      @RequestParam(value = "user", required = false) String userName,
      @RequestParam(value = "verificationName", required = false) String verificationName,
      RedirectAttributes redirectAttributes) {

    getLogger().debug("deleteUser({}, {}, {}, {}, {}, {}, {})",
        page, size, sort, query, scope, userName, verificationName);
    Map<String, Object> parameters = getParamterMap(page, size, sort, query, ou, scope);

    if (isEmpty(userName)) {
      String defaultMsg = "Deleting user failed. Username is required.";
      RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.DANGER, defaultMsg, "todo");
      redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);
      String redirect = getRedirectUri("users", PAGE_AND_OU_PARAMS, parameters);
      logRedirectTo(defaultMsg, redirect);
      return redirect;
    }

    Map<String, Object> parametersWithUsername = addToMap(parameters, "userName", userName);

    if (!userName.equalsIgnoreCase(verificationName)) {
      String defaultMsg = "Deleting user failed. The given username doesn't match.";
      RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.DANGER, defaultMsg, "todo");
      redirectAttributes.addFlashAttribute("dmsg", rmsg);
      String redirect = getRedirectUri("user-edit-misc?user={{userName}}",
          PAGE_AND_OU_PARAMS, parametersWithUsername);
      logRedirectTo(defaultMsg, redirect);
      return redirect;
    }

    if (!domainUserService.deleteUser(userName)) {
      String defaultMsg = "Deleting user failed. It is still present.";
      RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.WARNING, defaultMsg, "todo");
      redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);
      String redirect = getRedirectUri("user-edit-misc?user={{userName}}",
          PAGE_AND_OU_PARAMS, parametersWithUsername);
      logRedirectTo(defaultMsg, redirect);
      return redirect;
    }

    String defaultMsg = "User was successfully deleted.";
    RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.SUCCESS, defaultMsg, "todo");
    redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);
    String redirect = getRedirectUri("users",
        PAGE_AND_OU_PARAMS, parametersWithUsername);
    logRedirectTo(defaultMsg, redirect);
    return redirect;
  }

}
