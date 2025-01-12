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
import org.bremersee.dccon.controller.ui.components.OrganizationalUnitComponent;
import org.bremersee.dccon.controller.ui.components.PageableComponent;
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
public class UserEditMembershipsController extends AbstractEditController
    implements PageableComponent, OrganizationalUnitComponent {

  private final DomainUserService domainUserService;

  private final DomainGroupService domainGroupService;

  public UserEditMembershipsController(
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

  @GetMapping(path = "/admin/user-edit-memberships-direct")
  public String displayUserEditMembershipsDirect(
      @RequestParam(value = "user", required = false) String userName,
      @RequestParam(value = OU, required = false) Dn ou,
      @RequestParam(value = SCOPE, required = false) SearchScope searchScope,
      ModelMap model) {

    return displayUserEditMemberships(true, userName, ou, searchScope, model);
  }

  @GetMapping(path = "/admin/user-edit-memberships-resolved")
  public String displayUserEditMembershipsResolved(
      @RequestParam(value = "user", required = false) String userName,
      @RequestParam(value = OU, required = false) Dn ou,
      @RequestParam(value = SCOPE, required = false) SearchScope searchScope,
      ModelMap model) {

    return displayUserEditMemberships(false, userName, ou, searchScope, model);
  }

  private String displayUserEditMemberships(
      boolean direct,
      String userName,
      Dn ou,
      SearchScope searchScope,
      ModelMap model) {

    return Optional.ofNullable(userName)
        .flatMap(name -> domainUserService.getUser(userName, ou, searchScope))
        .map(user -> {
          model.addAttribute("user", user);
          Stream<DomainGroup> memberships;
          String page;
          if (direct) {
            memberships = domainGroupService.getMemberships(userName, ou, searchScope);
            page = "admin/user-edit-memberships-direct";
          } else {
            memberships = domainGroupService.resolveMemberships(userName, ou, searchScope);
            page = "admin/user-edit-memberships-resolved";
          }
          model.addAttribute("memberships", memberships.toList());
          return page;
        })
        .orElseGet(() -> entityNotFoundRedirect(model, "User", "todo", userName, "users"));
  }

}
