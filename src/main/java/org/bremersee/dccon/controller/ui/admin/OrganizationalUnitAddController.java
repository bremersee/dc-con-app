/*
 * Copyright 2025 the original author or authors.
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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.AbstractController;
import org.bremersee.dccon.controller.ui.components.PageableComponent;
import org.bremersee.dccon.controller.ui.components.RedirectComponent;
import org.bremersee.dccon.controller.ui.model.OrganizationalUnitAddRequest;
import org.bremersee.dccon.controller.ui.model.RedirectMessage;
import org.bremersee.dccon.controller.ui.model.RedirectMessageType;
import org.bremersee.dccon.model.OrganizationalUnit;
import org.bremersee.dccon.service.OrganizationalUnitService;
import org.bremersee.exception.ServiceException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The type OrganizationalUnitAddController.
 *
 * @author Christian Bremer
 */
@Controller
public class OrganizationalUnitAddController extends AbstractController
    implements PageableComponent, RedirectComponent {

  private final OrganizationalUnitService organizationalUnitService;

  @Override
  public String getDefaultSort() {
    return OU_SORT;
  }

  public OrganizationalUnitAddController(
      DomainControllerProperties properties,
      LocaleResolver localeResolver,
      OrganizationalUnitService organizationalUnitService) {
    super(properties, localeResolver);
    this.organizationalUnitService = organizationalUnitService;
  }

  @ModelAttribute("ous")
  public List<OrganizationalUnit> addOrganisationalUnits() {
    Stream<OrganizationalUnit> baseStream = Stream.of(organizationalUnitService.getBase());
    Stream<OrganizationalUnit> otherParentsStream = organizationalUnitService
        .getOrganizationalUnits()
        .filter(ou -> !ou.getSystemOu());
    return Stream.concat(baseStream, otherParentsStream).toList();
  }

  @GetMapping(path = "/admin/organizational-unit-add")
  public String displayOrganizationalUnitAdd(ModelMap model) {
    getLogger().debug("displayOrganizationalUnitAdd()");
    OrganizationalUnitAddRequest ouAddRequest = new OrganizationalUnitAddRequest();
    ouAddRequest.setParentOu(getProperties().getBaseDn().format());
    model.addAttribute("ouAddRequest", ouAddRequest);
    return "admin/organizational-unit-add";
  }

  @PostMapping(path = "/admin/organizational-unit-add")
  public String addOrganizationalUnit(
      @ModelAttribute(name = "ouAddRequest") OrganizationalUnitAddRequest ouAddRequest,
      ModelMap model,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {

    getLogger().debug("addOrganizationalUnit({})", ouAddRequest);

    OrganizationalUnit organizationalUnit = new OrganizationalUnit();
    organizationalUnit.setName(ouAddRequest.getName());
    organizationalUnit.setDescription(ouAddRequest.getDescription());

    OrganizationalUnit addedOu;
    try {
      addedOu = organizationalUnitService.add(organizationalUnit, ouAddRequest.getParentOuDn());

    } catch (ServiceException e) {
      handleException(bindingResult, e);
      getLogger().debug("Adding organizational unit failed. Some fields were invalid.");
      return "admin/organizational-unit-add";
    }

    model.clear();
    String name = addedOu.getName();
    RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.SUCCESS,
        String.format("Organizational unit '%s' was successfully added.", name),
        "todo", name);
    redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);

    Map<String, Object> parameters = getParamterMap();
    String redirect = getRedirectUri(
        "organizational-unit-edit?name={{name}}",
        PAGE_AND_OU_PARAMS,
        putToParameterMap(parameters, "name", addedOu.getDistinguishedNameUnformatted()));
    logRedirectTo("Organizational unit successfully added.", redirect);
    return redirect;
  }

  private void handleException(BindingResult bindingResult, ServiceException serviceException) {

    Object bindTarget = bindingResult.getTarget();
    getLogger().debug("handleException of bind target '{}'", bindTarget, serviceException);
    Assert.isTrue(bindTarget instanceof OrganizationalUnitAddRequest, "Illegal bind target.");
    String errorCode = Objects.requireNonNullElse(serviceException.getErrorCode(), "");
    switch (errorCode) {
      case EC_OU_NAME_REQUIRED: {
        bindingResult.rejectValue("name", "code",
            "Name of organizational unit is required.");
        break;
      }
      case EC_ILLEGAL_OU_NAME: {
        bindingResult.rejectValue("name", "code",
            "Name of organizational unit contains illegal characters.");
        break;
      }
      case EC_OU_ALREADY_EXISTS: {
        bindingResult.rejectValue("name", "code",
            "Organizational unit already exists.");
        break;
      }
      default: {
        getLogger().error("Adding group failed with a not mapped exception.", serviceException);
        throw serviceException;
      }
    }
  }

}
