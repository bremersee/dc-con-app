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

import static org.bremersee.dccon.ErrorCode.EC_ADDING_USER_FAILED;
import static org.bremersee.dccon.ErrorCode.EC_SAM_ACCOUNT_ALREADY_EXISTS;
import static org.bremersee.dccon.ErrorCode.EC_PASSWORD_RESTRICTIONS;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.comparator.model.SortOrders;
import org.bremersee.dccon.controller.ui.AbstractController;
import org.bremersee.dccon.controller.ui.RedirectMessage;
import org.bremersee.dccon.controller.ui.RedirectMessageType;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.service.DomainGroupService;
import org.bremersee.dccon.service.DomainService;
import org.bremersee.dccon.service.DomainUserService;
import org.bremersee.exception.ServiceException;
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
@Slf4j
public class UserAddController extends AbstractController {

  private final DomainService domainService;

  private final DomainUserService domainUserService;

  private final DomainGroupService domainGroupService;

  public UserAddController(LocaleResolver localeResolver,
      DomainService domainService,
      DomainUserService domainUserService,
      DomainGroupService domainGroupService) {
    super(localeResolver);
    this.domainService = domainService;
    this.domainUserService = domainUserService;
    this.domainGroupService = domainGroupService;
  }

  @ModelAttribute("passwordPattern")
  public String getPasswordPattern() {
    return domainService.getPasswordInformation().getPasswordRegex();
  }

  @ModelAttribute("sendEmail")
  public boolean getSendEmail() {
    return false;
  }

  @GetMapping(path = "/admin/user-add")
  public String displayUserAdd(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "2147483647") int size,
      @RequestParam(name = "sort", defaultValue = "lastName,asc;firstName,asc;samAccountName,asc") SortOrders sort,
      @RequestParam(name = "q", required = false) String query,
      ModelMap model) {

    addPageRequest(model, page, size, sort, query);
    if (!model.containsAttribute("user")) {
      DomainUser domainUser = new DomainUser();
      model.addAttribute("user", domainUser);
    }
    return "admin/user-add";
  }

  @PostMapping(path = "/admin/user-add")
  public String addUser(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "2147483647") int size,
      @RequestParam(name = "sort", defaultValue = "lastName,asc;firstName,asc;samAccountName,asc") SortOrders sort,
      @RequestParam(name = "q", required = false) String query,
      @RequestParam(name = "sendEmail", defaultValue = "false") boolean sendEmail,
      @ModelAttribute("user") DomainUser user,
      ModelMap model,
      HttpServletRequest request,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {

    return Optional.ofNullable(user)
        .map(usr -> doAddUser(usr, sendEmail, resolveLocale(request), bindingResult))
        .map(pair -> {
          if (pair.getSecond().hasErrors()) {
            addPageRequest(model, page, size, sort, query);
            model.addAttribute("sendEmail", sendEmail);
            return "admin/user-add";
          }
          DomainUser addedUser = pair.getFirst();
          model.clear();
          String msg = getMessageSource().getMessage(
              "i18n.user.added",
              new Object[]{addedUser.getDisplayName()},
              String.format("User '%s' was successfully added.", addedUser.getDisplayName()),
              resolveLocale(request));
          final RedirectMessage rmsg = new RedirectMessage(msg, RedirectMessageType.SUCCESS);
          redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);
          return redirect("/admin/user-edit?user=" + encodeUrlParameter(addedUser.getSamAccountName()),
              page, size, sort, query);
        })
        .orElseGet(() -> redirect("/admin/users", page, size, sort, query));
  }

  private Pair<DomainUser, BindingResult> doAddUser(DomainUser user, boolean sendEmail,
      Locale locale, BindingResult bindingResult) {
    try {
      DomainUser addedUser = fake(); //domainUserService.addUser(user, sendEmail, locale);
      return Pair.of(addedUser, bindingResult);

    } catch (ServiceException serviceException) {
      String errorCode = Objects.requireNonNullElse(serviceException.getErrorCode(), "");
      switch (errorCode) {
        case EC_SAM_ACCOUNT_ALREADY_EXISTS: {
          bindingResult.rejectValue("userName", "code",
              "User name already exists.");
          break;
        }
        case EC_PASSWORD_RESTRICTIONS: {
          bindingResult.rejectValue("userName", "code",
              "Password restrictions are not met.");
          break;
        }
        case EC_ADDING_USER_FAILED: {
          bindingResult.rejectValue("userName", "code",
              "Something went wrong. Please try again later.");
        }
      }
    }
    return Pair.of(user, bindingResult);
  }

  private DomainUser fake() {
    throw ServiceException.alreadyExistsWithErrorCode("", "", EC_SAM_ACCOUNT_ALREADY_EXISTS);
  }

}
