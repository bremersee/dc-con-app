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
import java.util.Optional;
import java.util.stream.Stream;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.components.PageableComponent;
import org.bremersee.dccon.controller.ui.components.RedirectComponent;
import org.bremersee.dccon.controller.ui.model.OrganizationalUnitEditRequest;
import org.bremersee.dccon.controller.ui.model.RedirectMessage;
import org.bremersee.dccon.controller.ui.model.RedirectMessageType;
import org.bremersee.dccon.model.OrganizationalUnit;
import org.bremersee.dccon.service.OrganizationalUnitService;
import org.bremersee.exception.ServiceException;
import org.ldaptive.dn.Dn;
import org.ldaptive.dn.NameValue;
import org.ldaptive.dn.RDn;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The type OrganizationalUnitAddController.
 *
 * @author Christian Bremer
 */
@Controller
public class OrganizationalUnitEditController extends AbstractEditController
    implements PageableComponent, RedirectComponent {

  private final OrganizationalUnitService organizationalUnitService;

  @Override
  public String getDefaultSort() {
    return OU_SORT;
  }

  public OrganizationalUnitEditController(
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

  @GetMapping(path = "/admin/organizational-unit-edit")
  public String displayOrganizationalUnitEdit(
      @RequestParam(value = "name", required = false) Dn ouDn,
      ModelMap model,
      RedirectAttributes redirectAttributes) {

    getLogger().debug("displayOrganizationalUnitEdit({})", ouDn);
    String name = Optional.ofNullable(ouDn)
        .map(Dn::getRDn)
        .map(RDn::getNameValue)
        .map(NameValue::getStringValue)
        .orElse("null");

    return Optional.ofNullable(ouDn)
        .filter(dn -> !dn.isEmpty())
        .flatMap(organizationalUnitService::getOrganizationalUnit)
        .map(ou -> {
          model.addAttribute("organizationalUnit", ou);
          OrganizationalUnitEditRequest ouEditRequest = new OrganizationalUnitEditRequest(ou);
          model.put("ouEditRequest", ouEditRequest);
          return "admin/organizational-unit-edit";
        })
        .orElseGet(() -> entityNotFoundRedirect(
            redirectAttributes, "Organizational Unit", "todo", name,
            "organizational-units"));
  }

  @PostMapping(path = "/admin/organizational-unit-edit")
  public String updateOrganizationalUnit(
      @ModelAttribute(name = "ouEditRequest") OrganizationalUnitEditRequest ouEditRequest,
      ModelMap model,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {

    getLogger().debug("editOrganizationalUnit({})", ouEditRequest);

    String name = Optional.ofNullable(ouEditRequest.getOu())
        .map(Dn::new)
        .map(Dn::getRDn)
        .map(RDn::getNameValue)
        .map(NameValue::getStringValue)
        .orElse("null");

    return Optional.ofNullable(ouEditRequest.getOu())
        .map(Dn::new)
        .flatMap(organizationalUnitService::getOrganizationalUnit)
        .map(ou -> updateOrganizationalUnit(
            ou, ouEditRequest, model, bindingResult, redirectAttributes))
        .orElseGet(() -> entityNotFoundRedirect(
            redirectAttributes, "Organizational Unit", "todo", name,
            "organizational-units"));
  }

  private String updateOrganizationalUnit(
      OrganizationalUnit ou,
      OrganizationalUnitEditRequest ouEditRequest,
      ModelMap model,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {

    ouEditRequest.update(ou);
    OrganizationalUnit updatedOu;
    try {
      updatedOu = organizationalUnitService.update(ou, ouEditRequest.getParentOuDn());

    } catch (ServiceException e) {
      handleException(bindingResult, e);
      getLogger().debug("Updating organizational unit failed. Some fields were invalid.");
      return "admin/organizational-unit-edit";
    }

    model.clear();
    String newName = updatedOu.getName();
    RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.SUCCESS,
        String.format("Organizational unit '%s' was successfully updated.", newName),
        "todo", newName);
    redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);

    Map<String, Object> parameters = getParamterMap();
    String redirect = getRedirectUri(
        "organizational-unit-edit?name={{name}}",
        PAGE_AND_OU_PARAMS,
        putToParameterMap(parameters, "name", updatedOu.getDistinguishedNameUnformatted()));
    logRedirectTo("Organizational unit successfully added.", redirect);
    return redirect;
  }

  private void handleException(BindingResult bindingResult, ServiceException serviceException) {

    Object bindTarget = bindingResult.getTarget();
    getLogger().debug("handleException of bind target '{}'", bindTarget, serviceException);
    Assert.isTrue(bindTarget instanceof OrganizationalUnitEditRequest, "Illegal bind target.");
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
        bindingResult.rejectValue("parentOu", "code",
            "Organizational unit already exists.");
        break;
      }
      case EC_ILLEGAL_SYSTEM_ENTITY_OPERATION: {
        bindingResult.rejectValue("name", "code",
            "Organizational unit is a critical system object. Moving and renaming is permitted.");
        bindingResult.rejectValue("parentOu", "code",
            "Organizational unit is a critical system object. Moving and renaming is permitted.");
        break;
      }
      default: {
        getLogger().error("Adding group failed with a not mapped exception.", serviceException);
        throw serviceException;
      }
    }
  }

}
