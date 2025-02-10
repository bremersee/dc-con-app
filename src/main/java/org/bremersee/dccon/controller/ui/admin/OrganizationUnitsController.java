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

import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.AbstractController;
import org.bremersee.dccon.controller.ui.CurrentPageNameProvider;
import org.bremersee.dccon.controller.ui.components.PageableComponent;
import org.bremersee.dccon.model.OrganizationUnitPage;
import org.bremersee.dccon.service.OrganizationalUnitService;
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
public class OrganizationUnitsController extends AbstractController
    implements PageableComponent, CurrentPageNameProvider {

  private final OrganizationalUnitService organizationalUnitService;

  public OrganizationUnitsController(
      DomainControllerProperties domainControllerProperties,
      LocaleResolver localeResolver,
      OrganizationalUnitService organizationalUnitService) {
    super(domainControllerProperties, localeResolver);
    this.organizationalUnitService = organizationalUnitService;
  }

  @Override
  public String getDefaultSort() {
    return OU_SORT;
  }

  @Override
  public String getCurrentPageName() {
    return "organizational-units";
  }

  @RequestMapping(path = "/admin/organizational-units", method = RequestMethod.GET)
  public String displayOrganizationalUnits(
      @RequestParam(name = PAGE, defaultValue = PAGE_DEFAULT) int page,
      @RequestParam(name = SIZE, defaultValue = SIZE_DEFAULT) int size,
      @RequestParam(name = QUERY, required = false) String query,
      ModelMap model) {

    Pageable pageable = PageRequest.of(page, size);
    OrganizationUnitPage ouPage = new OrganizationUnitPage(
        organizationalUnitService.getOrganizationalUnits(pageable, query));
    model.put("ouPage", ouPage);
    return "admin/organizational-units";
  }

}
