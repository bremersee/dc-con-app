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
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.AbstractController;
import org.bremersee.dccon.controller.ui.model.RedirectMessage;
import org.bremersee.dccon.controller.ui.model.RedirectMessageType;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.service.DomainGroupService;
import org.bremersee.dccon.service.DomainService;
import org.bremersee.dccon.service.DomainUserService;
import org.springframework.data.util.Pair;
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
public class GroupAddController extends AbstractController {

  private final DomainService domainService;

  private final DomainUserService domainUserService;

  private final DomainGroupService domainGroupService;

  public GroupAddController(
      DomainControllerProperties domainControllerProperties,
      LocaleResolver localeResolver,
      DomainService domainService,
      DomainUserService domainUserService,
      DomainGroupService domainGroupService) {
    super(domainControllerProperties, localeResolver);
    this.domainService = domainService;
    this.domainUserService = domainUserService;
    this.domainGroupService = domainGroupService;
  }

  @GetMapping(path = "/admin/group-add")
  public String displayUserAdd(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "2147483647") int size,
      @RequestParam(name = "sort", defaultValue = "name") SortOrders sort,
      @RequestParam(name = "q", required = false) String query,
      ModelMap model) {

    addPageRequest(model, page, size, sort, query);
    if (!model.containsAttribute("group")) {
      DomainGroup domainGroup = new DomainGroup();
      model.addAttribute("group", domainGroup);
    }
    return "admin/group-add";
  }

  @PostMapping(path = "/admin/group-add")
  public String addUser(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "2147483647") int size,
      @RequestParam(name = "sort", defaultValue = "samAccountName") SortOrders sort,
      @RequestParam(name = "q", required = false) String query,
      @ModelAttribute("group") DomainGroup group,
      ModelMap model,
      HttpServletRequest request,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {

    return Optional.ofNullable(group)
        .map(grp -> doAddGroup(grp, bindingResult))
        .map(pair -> {
          if (pair.getSecond().hasErrors()) {
            addPageRequest(model, page, size, sort, query);
            return "admin/group-add";
          }
          DomainGroup addedGroup = pair.getFirst();
          model.clear();
          String msg = getMessageSource().getMessage(
              "i18n.user.added",
              new Object[]{addedGroup.getSamAccountName()},
              String.format("Group '%s' was successfully added.", addedGroup.getSamAccountName()),
              resolveLocale(request));
          final RedirectMessage rmsg = new RedirectMessage(msg, RedirectMessageType.SUCCESS);
          redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);
          return redirect("/admin/group-edit?name=" + encodeUrlParameter(addedGroup.getSamAccountName()),
              page, size, sort, query);
        })
        .orElseGet(() -> redirect("/admin/groups", page, size, sort, query));
  }

  private Pair<DomainGroup, BindingResult> doAddGroup(
      DomainGroup group, BindingResult bindingResult) {
    return Pair.of(group, bindingResult);
    /*
    try {
      DomainUser addedUser = fake(); //domainUserService.addUser(user, sendEmail, locale);
      return Pair.of(addedUser, bindingResult);

    } catch (ServiceException serviceException) {
      String errorCode = Objects.requireNonNullElse(serviceException.getErrorCode(), "");
      switch (errorCode) {
        case NAME_ALREADY_EXISTS: {
          bindingResult.rejectValue("userName", "code",
              "User name already exists.");
          break;
        }
        case PASSWORD_RESTRICTIONS: {
          bindingResult.rejectValue("userName", "code",
              "Password restrictions are not met.");
          break;
        }
        case ADDING_USER_FAILED: {
          bindingResult.rejectValue("userName", "code",
              "Something went wrong. Please try again later.");
        }
      }
    }
    return Pair.of(user, bindingResult);
    */
  }

}
