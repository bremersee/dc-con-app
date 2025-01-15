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
import org.bremersee.dccon.model.TreeSearchScope;
import org.bremersee.dccon.service.DomainGroupService;
import org.ldaptive.dn.Dn;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;

/**
 * The type GroupsController.
 *
 * @author Christian Bremer
 */
@Controller
public class GroupMembershipsController extends AbstractEditController
    implements PageableComponent, OrganizationalUnitComponent {

  private final DomainGroupService domainGroupService;

  public GroupMembershipsController(
      DomainControllerProperties domainControllerProperties,
      LocaleResolver localeResolver,
      DomainGroupService domainGroupService) {
    super(domainControllerProperties, localeResolver);
    this.domainGroupService = domainGroupService;
  }

  @Override
  public String getDefaultSort() {
    return USER_SORT;
  }

  @GetMapping(path = "/admin/group-memberships-direct")
  public String displayGroupEditMembershipsDirect(
      @RequestParam(value = "name", required = false) String groupName,
      @RequestParam(value = OU, required = false) Dn ou,
      @RequestParam(value = SCOPE, required = false) TreeSearchScope searchScope,
      ModelMap model) {

    return displayGroupEditMemberships(true, groupName, ou, searchScope, model);
  }

  @GetMapping(path = "/admin/group-memberships-resolved")
  public String displayGroupEditMembershipsResolved(
      @RequestParam(value = "name", required = false) String groupName,
      @RequestParam(value = OU, required = false) Dn ou,
      @RequestParam(value = SCOPE, required = false) TreeSearchScope searchScope,
      ModelMap model) {

    return displayGroupEditMemberships(false, groupName, ou, searchScope, model);
  }

  private String displayGroupEditMemberships(
      boolean direct,
      String groupName,
      Dn ou,
      TreeSearchScope searchScope,
      ModelMap model) {

    return Optional.ofNullable(groupName)
        .flatMap(name -> domainGroupService.getGroup(groupName, ou, searchScope))
        .map(group -> {
          model.addAttribute("group", group);
          Stream<DomainGroup> memberships;
          String page;
          if (direct) {
            memberships = domainGroupService.getMemberships(groupName, ou, searchScope);
            page = "admin/group-memberships-direct";
          } else {
            memberships = domainGroupService.resolveMemberships(groupName, ou, searchScope);
            page = "admin/group-memberships-resolved";
          }
          model.addAttribute("memberships", memberships.toList());
          return page;
        })
        .orElseGet(() -> entityNotFoundRedirect(model, "Group", "todo", groupName, "groups"));
  }

}
