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

import java.util.Optional;
import java.util.stream.Stream;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.AbstractController;
import org.bremersee.dccon.controller.ui.components.OrganizationalUnitComponent;
import org.bremersee.dccon.controller.ui.components.PageableComponent;
import org.bremersee.dccon.controller.ui.components.RedirectComponent;
import org.bremersee.dccon.controller.ui.model.RedirectMessage;
import org.bremersee.dccon.controller.ui.model.RedirectMessageType;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.service.DomainGroupService;
import org.bremersee.dccon.service.DomainUserService;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;

/**
 * The type UsersController.
 *
 * @author Christian Bremer
 */
@Controller
public class UserEditMembershipController extends AbstractController implements PageableComponent,
    RedirectComponent, OrganizationalUnitComponent {

  private final DomainUserService domainUserService;

  private final DomainGroupService domainGroupService;

  public UserEditMembershipController(
      DomainControllerProperties domainControllerProperties,
      LocaleResolver localeResolver,
      DomainUserService domainUserService,
      DomainGroupService domainGroupService) {
    super(domainControllerProperties, localeResolver);
    this.domainUserService = domainUserService;
    this.domainGroupService = domainGroupService;
  }

  @Override
  public String getDefaultSort() {
    return USER_SORT;
  }

  @GetMapping(path = "/admin/user-edit-membership")
  public String displayUserEditMembership(
      @RequestParam(value = "user", required = false) String userName,
      @RequestParam(value = "resolved", required = false) Boolean resolved,
      @RequestParam(value = OU, required = false) Dn ou,
      @RequestParam(value = SCOPE, required = false) SearchScope searchScope,
      ModelMap model) {

    return Optional.ofNullable(userName)
        .flatMap(name -> domainUserService.getUser(userName, ou, searchScope))
        .map(user -> {
          model.addAttribute("user", user);
          boolean isResolved = Boolean.TRUE.equals(resolved);
          model.addAttribute("resolved", isResolved);
          Stream<DomainGroup> membership;
          if (isResolved) {
            // resolved group memberships
            membership = domainGroupService.resolveMembership(userName, ou, searchScope);
          } else {
            // direct group memberships
            membership = domainGroupService.getMembership(userName, ou, searchScope);
          }
          model.addAttribute("membership", membership.sorted().toList());
          return "admin/user-edit-membership";
        })
        .orElseGet(() -> {
          String msg = String.format("User '%s' not found.", userName);
          model.addAttribute("rmsg", new RedirectMessage(msg, RedirectMessageType.WARNING));
          return "admin/user-edit-membership";
        });
  }

}
