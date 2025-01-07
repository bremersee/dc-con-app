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

import lombok.Getter;
import org.bremersee.comparator.model.SortOrders;
import org.bremersee.comparator.spring.mapper.SortMapper;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.AbstractController;
import org.bremersee.dccon.controller.ui.components.OrganizationalUnitNavigationComponent;
import org.bremersee.dccon.controller.ui.components.PageableComponent;
import org.bremersee.dccon.controller.ui.model.OrganizationalUnitDropdown;
import org.bremersee.dccon.model.DomainGroupPage;
import org.bremersee.dccon.service.DomainGroupService;
import org.bremersee.dccon.service.OrganizationalUnitService;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;

/**
 * The type UsersController.
 *
 * @author Christian Bremer
 */
@Controller
public class GroupsController extends AbstractController
    implements PageableComponent, OrganizationalUnitNavigationComponent {

  private final DomainGroupService domainGroupService;

  @Getter
  private final OrganizationalUnitService organizationalUnitService;

  public GroupsController(
      DomainControllerProperties domainControllerProperties,
      LocaleResolver localeResolver,
      DomainGroupService domainGroupService,
      OrganizationalUnitService organizationalUnitService) {
    super(domainControllerProperties, localeResolver);
    this.domainGroupService = domainGroupService;
    this.organizationalUnitService = organizationalUnitService;
  }

  @Override
  public Dn getDefaultOrganizationalUnit() {
    return getProperties().getGroup().getDefaultGroupOu();
  }

  @Override
  public SearchScope getDefaultSearchScope() {
    return getProperties().getGroup().getDefaultGroupSearchScope();
  }

  @Override
  public String getDefaultSort() {
    return GROUP_SORT;
  }

  @Override
  public String getCurrentPageName() {
    return "groups";
  }

  @RequestMapping(path = "/admin/groups", method = {RequestMethod.GET, RequestMethod.POST})
  public String displayGroups(
      @RequestParam(name = PAGE, defaultValue = PAGE_DEFAULT) int page,
      @RequestParam(name = SIZE, defaultValue = SIZE_DEFAULT) int size,
      @RequestParam(name = SORT, defaultValue = GROUP_SORT) SortOrders sort,
      @RequestParam(name = QUERY, required = false) String query,
      @RequestParam(name = OU, required = false) Dn ou,
      @RequestParam(name = SCOPE, required = false) SearchScope scope,
      ModelMap model) {

    OrganizationalUnitDropdown ouDropdown = getOrganizationalUnitDropdown(ou, scope);
    addOrganizationalUnitDropdown(model, ouDropdown);
    Pageable pageable = PageRequest.of(page, size, SortMapper.toSort(sort));
    DomainGroupPage groupPage = new DomainGroupPage(domainGroupService.getGroups(pageable, query, ou, ouDropdown.getSelectedScope()));
    model.addAttribute("groups", groupPage);
    return "admin/groups";
  }

}
