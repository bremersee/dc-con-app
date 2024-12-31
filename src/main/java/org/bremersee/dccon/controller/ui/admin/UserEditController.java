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

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.bremersee.comparator.model.SortOrders;
import org.bremersee.dccon.controller.ui.AbstractController;
import org.bremersee.dccon.controller.ui.RedirectMessage;
import org.bremersee.dccon.controller.ui.RedirectMessageType;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.service.DomainGroupService;
import org.bremersee.dccon.service.DomainUserService;
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
public class UserEditController extends AbstractController {

  private final DomainUserService domainUserService;

  private final DomainGroupService domainGroupService;

  public UserEditController(LocaleResolver localeResolver,
      DomainUserService domainUserService,
      DomainGroupService domainGroupService) {
    super(localeResolver);
    this.domainUserService = domainUserService;
    this.domainGroupService = domainGroupService;
  }

  @GetMapping(path = "/admin/user-edit")
  public String displayUserEdit(
      @RequestParam(value = "user", required = false) String userName,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "2147483647") int size,
      @RequestParam(name = "sort", defaultValue = "lastName,asc;firstName,asc;samAccountName,asc") SortOrders sort,
      @RequestParam(name = "q", required = false) String query,
      ModelMap model) {

    addPageRequest(model, page, size, sort, query);
    if (!model.containsAttribute("user")) {
      return Optional.ofNullable(userName)
          .flatMap(name ->  domainUserService.getUser(name, null, null)) // TODO
          .map(user -> {
            model.addAttribute("user", user);
            //addGroups(user, model);
            return "admin/user-edit";
          })
          .orElseGet(() -> redirect("/admin/users", page, size, sort, query));

    }
    return "admin/user-edit";
  }

  /*
  private void addGroups(DomainUser user, ModelMap model) {
    Set<String> userGroups = new HashSet<>(user.getGroups());
    List<SelectOption> selectOptions = domainGroupService
        .getGroups(PageRequest.of(0, Integer.MAX_VALUE), null)
        .stream()
        .map(group -> new SelectOption(
            group.getSamAccountName(), group.getSamAccountName(), userGroups.contains(group.getSamAccountName())))
        .sorted()
        .toList();
    model.addAttribute("groups", selectOptions);
  }
  */

  @PostMapping(path = "/admin/user-edit")
  public String updateUser(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "2147483647") int size,
      @RequestParam(name = "sort", defaultValue = "lastName,asc;firstName,asc;samAccountName,asc") SortOrders sort,
      @RequestParam(name = "q", required = false) String query,
      @ModelAttribute("user") DomainUser user,
      ModelMap model,
      HttpServletRequest request,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {

    return Optional.ofNullable(user)
        .flatMap(usr ->domainUserService.updateUser(usr.getSamAccountName(), usr))
        .map(updatedUser -> {
          model.clear();
          String msg = getMessageSource().getMessage(
              "i18n.user.updated",
              new Object[]{updatedUser.getDisplayName()},
              String.format("User '%s' was successfully updated.", updatedUser.getDisplayName()),
              resolveLocale(request));
          final RedirectMessage rmsg = new RedirectMessage(msg, RedirectMessageType.SUCCESS);
          redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);
          return redirect("/admin/users", page, size, sort, query);
        })
        .orElseGet(() -> redirect("/admin/users", page, size, sort, query));
  }
}
