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
import org.bremersee.dccon.service.DomainComputerService;
import org.bremersee.dccon.service.DomainGroupService;
import org.ldaptive.dn.Dn;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;

/**
 * The type ComputersController.
 *
 * @author Christian Bremer
 */
@Controller
public class ComputerEditMembershipsController extends AbstractEditController
    implements PageableComponent, OrganizationalUnitComponent {

  private final DomainComputerService domainComputerService;

  private final DomainGroupService domainGroupService;

  public ComputerEditMembershipsController(
      DomainControllerProperties domainControllerProperties,
      LocaleResolver localeResolver,
      DomainComputerService domainComputerService,
      DomainGroupService domainGroupService) {
    super(domainControllerProperties, localeResolver);
    this.domainComputerService = domainComputerService;
    this.domainGroupService = domainGroupService;
  }

  @Override
  public String getDefaultSort() {
    return COMPUTER_SORT;
  }

  @GetMapping(path = "/admin/computer-edit-memberships-direct")
  public String displayComputerEditMembershipsDirect(
      @RequestParam(value = "name", required = false) String computerName,
      @RequestParam(value = OU, required = false) Dn ou,
      @RequestParam(value = SCOPE, required = false) TreeSearchScope searchScope,
      ModelMap model) {

    return displayComputerEditMemberships(true, computerName, ou, searchScope, model);
  }

  @GetMapping(path = "/admin/computer-edit-memberships-resolved")
  public String displayComputerEditMembershipsResolved(
      @RequestParam(value = "name", required = false) String computerName,
      @RequestParam(value = OU, required = false) Dn ou,
      @RequestParam(value = SCOPE, required = false) TreeSearchScope searchScope,
      ModelMap model) {

    return displayComputerEditMemberships(false, computerName, ou, searchScope, model);
  }

  private String displayComputerEditMemberships(
      boolean direct,
      String computerName,
      Dn ou,
      TreeSearchScope searchScope,
      ModelMap model) {

    return Optional.ofNullable(computerName)
        .flatMap(name -> domainComputerService.getComputer(computerName, ou, searchScope))
        .map(computer -> {
          model.addAttribute("computer", computer);
          Stream<DomainGroup> memberships;
          String page;
          if (direct) {
            memberships = domainGroupService.getMemberships(computerName, ou, searchScope);
            page = "admin/computer-edit-memberships-direct";
          } else {
            memberships = domainGroupService.resolveMemberships(computerName, ou, searchScope);
            page = "admin/computer-edit-memberships-resolved";
          }
          model.addAttribute("memberships", memberships.toList());
          return page;
        })
        .orElseGet(() -> entityNotFoundRedirect(model, "Group", "todo", computerName, "computers"));
  }

}
