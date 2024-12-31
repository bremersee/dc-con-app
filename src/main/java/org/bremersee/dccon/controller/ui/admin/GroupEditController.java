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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.bremersee.comparator.model.SortOrders;
import org.bremersee.dccon.controller.ui.AbstractController;
import org.bremersee.dccon.controller.ui.RedirectMessage;
import org.bremersee.dccon.controller.ui.RedirectMessageType;
import org.bremersee.dccon.controller.ui.SelectOption;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.service.DomainGroupService;
import org.bremersee.dccon.service.DomainUserService;
import org.springframework.data.domain.PageRequest;
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
public class GroupEditController extends AbstractController {

  private final DomainUserService domainUserService;

  private final DomainGroupService domainGroupService;

  public GroupEditController(LocaleResolver localeResolver,
      DomainUserService domainUserService,
      DomainGroupService domainGroupService) {
    super(localeResolver);
    this.domainUserService = domainUserService;
    this.domainGroupService = domainGroupService;
  }

  @GetMapping(path = "/admin/group-edit")
  public String displayGroupEdit(
      @RequestParam(value = "name", required = false) String groupName,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "2147483647") int size,
      @RequestParam(name = "sort", defaultValue = "name") SortOrders sort,
      @RequestParam(name = "q", required = false) String query,
      ModelMap model) {

    addPageRequest(model, page, size, sort, query);
    if (!model.containsAttribute("group")) {
      return Optional.ofNullable(groupName)
          .flatMap(name -> domainGroupService.getGroup(name, null, null)) // TODO
          .map(group -> {
            model.addAttribute("group", group);
            addMembers(group, model);
            return "admin/group-edit";
          })
          .orElseGet(() -> redirect("/admin/groups", page, size, sort, query));

    }
    return "admin/group-edit";
  }

  private void addMembers(DomainGroup group, ModelMap model) {
    Set<String> members = new HashSet<>(group.getMembers());
    List<SelectOption> selectOptions = domainUserService
        .getUsers(PageRequest.of(0, Integer.MAX_VALUE), null, null, null)
        .stream()
        .map(user -> new SelectOption(
            user.getSamAccountName(), getDisplayName(user), members.contains(user.getSamAccountName())))
        .sorted()
        .toList();
    model.addAttribute("members", selectOptions);
  }

  private String getDisplayName(DomainUser user) {
    return Optional.ofNullable(user.getDisplayName())
        .filter(name -> !name.isBlank())
        .orElseGet(user::getSamAccountName);
  }

  @PostMapping(path = "/admin/group-edit")
  public String updateGroup(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "2147483647") int size,
      @RequestParam(name = "sort", defaultValue = "samAccountName,asc") SortOrders sort,
      @RequestParam(name = "q", required = false) String query,
      @ModelAttribute("group") DomainGroup group,
      ModelMap model,
      HttpServletRequest request,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {

    return Optional.ofNullable(group)
        .flatMap(grp ->domainGroupService.updateGroup(grp.getSamAccountName(), grp))
        .map(updatedGroup -> {
          model.clear();
          String msg = getMessageSource().getMessage(
              "i18n.group.updated",
              new Object[]{updatedGroup.getSamAccountName()},
              String.format("Group '%s' was successfully updated.", updatedGroup.getSamAccountName()),
              resolveLocale(request));
          final RedirectMessage rmsg = new RedirectMessage(msg, RedirectMessageType.SUCCESS);
          redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);
          return redirect("/admin/groups", page, size, sort, query);
        })
        .orElseGet(() -> redirect("/admin/groups", page, size, sort, query));
  }
}
