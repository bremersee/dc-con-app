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

import java.util.Map;
import java.util.Optional;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.components.OrganizationalUnitComponent;
import org.bremersee.dccon.controller.ui.components.PageableComponent;
import org.bremersee.dccon.controller.ui.model.RedirectMessage;
import org.bremersee.dccon.controller.ui.model.RedirectMessageType;
import org.bremersee.dccon.controller.ui.model.SamAccountDeleteRequest;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.model.TreeSearchScope;
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
public class UserDeleteController extends AbstractEditController implements PageableComponent,
    OrganizationalUnitComponent {

  private final DomainUserService domainUserService;

  public UserDeleteController(
      DomainControllerProperties domainControllerProperties,
      LocaleResolver localeResolver,
      DomainUserService domainUserService) {
    super(domainControllerProperties, localeResolver);
    this.domainUserService = domainUserService;
  }

  @Override
  public String getDefaultSort() {
    return USER_SORT;
  }

  @GetMapping(path = "/admin/user-delete")
  public String displayUserDelete(
      @RequestParam(value = "user", required = false) String userName,
      @RequestParam(value = OU, required = false) Dn ou,
      @RequestParam(value = SCOPE, required = false) TreeSearchScope searchScope,
      ModelMap model,
      RedirectAttributes redirectAttributes) {

    return Optional.ofNullable(userName)
        .flatMap(name -> domainUserService.getUser(userName, ou, searchScope))
        .map(user -> {
          model.addAttribute("user", user);
          model.addAttribute("deleteRequest", new SamAccountDeleteRequest(user));
          return "admin/user-delete";
        })
        .orElseGet(() -> entityNotFoundRedirect(
            redirectAttributes, "User", "todo", userName, "users"));
  }

  @PostMapping(path = "/admin/user-delete")
  public String deleteUser(
      @RequestParam(value = OU, required = false) Dn ou,
      @RequestParam(value = SCOPE, required = false) TreeSearchScope searchScope,
      @ModelAttribute("deleteRequest") SamAccountDeleteRequest deleteRequest,
      ModelMap model,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {

    getLogger().debug("deleteUser({})", deleteRequest);
    return Optional.ofNullable(deleteRequest.getSamAccountName())
        .flatMap(name -> domainUserService.getUser(name, ou, searchScope))
        .map(user -> {
          if (!user.getSamAccountName().equalsIgnoreCase(deleteRequest.getVerificationName())) {
            bindingResult.rejectValue("verificationName", "todo", "The name doesn't match.");
            model.addAttribute("user", user);
            return "admin/user-delete";
          }
          return deleteUser(user, model, redirectAttributes);
        })
        .orElseGet(() -> entityNotFoundRedirect(
            redirectAttributes, "User", "todo", deleteRequest.getSamAccountName(),
            "users"));
  }

  private String deleteUser(
      DomainUser user,
      ModelMap model,
      RedirectAttributes redirectAttributes) {

    boolean result = domainUserService.deleteUser(user.getSamAccountName());
    model.clear();
    RedirectMessage rmsg;
    if (result) {
      rmsg = getRedirectMessage(RedirectMessageType.SUCCESS,
          String.format("User '%s' was successfully deleted.", user.getName()),
          "todo", user.getName());
    } else {
      rmsg = getRedirectMessage(RedirectMessageType.WARNING,
          String.format("Somehow the computer '%s' was not deleted.", user.getName()),
          "todo", user.getName());
    }
    redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);

    Map<String, Object> parameters = getParamterMap();
    String redirect = getRedirectUri("users", PAGE_AND_OU_PARAMS, parameters);
    logRedirectTo("User deletion message.", redirect);
    return redirect;
  }

}
