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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.Getter;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.components.OrganisationalUnitsComponent;
import org.bremersee.dccon.controller.ui.components.OrganizationalUnitComponent;
import org.bremersee.dccon.controller.ui.components.PageableComponent;
import org.bremersee.dccon.controller.ui.model.DomainComputerEditRequest;
import org.bremersee.dccon.controller.ui.model.RedirectMessage;
import org.bremersee.dccon.controller.ui.model.RedirectMessageType;
import org.bremersee.dccon.model.DomainComputer;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.service.DomainComputerService;
import org.bremersee.dccon.service.DomainGroupService;
import org.bremersee.dccon.service.DomainService;
import org.bremersee.dccon.service.OrganizationalUnitService;
import org.bremersee.exception.ServiceException;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
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
public class ComputerEditController extends AbstractEditController implements PageableComponent,
    OrganizationalUnitComponent, OrganisationalUnitsComponent {

  private final DomainService domainService;

  private final DomainComputerService domainComputerService;

  private final DomainGroupService domainGroupService;

  @Getter
  private final OrganizationalUnitService organizationalUnitService;

  public ComputerEditController(
      DomainControllerProperties domainControllerProperties,
      LocaleResolver localeResolver,
      DomainService domainService,
      DomainComputerService domainComputerService,
      DomainGroupService domainGroupService,
      OrganizationalUnitService organizationalUnitService) {
    super(domainControllerProperties, localeResolver);
    this.domainService = domainService;
    this.domainComputerService = domainComputerService;
    this.domainGroupService = domainGroupService;
    this.organizationalUnitService = organizationalUnitService;
  }

  @Override
  public String getDefaultSort() {
    return GROUP_SORT;
  }

  @ModelAttribute("rfc2307Enabled")
  public boolean isRfc2307Enabled() {
    return domainService.isRfc2307Enabled();
  }

  @GetMapping(path = "/admin/computer-edit")
  public String displayComputerEdit(
      @RequestParam(value = "name", required = false) String computerName,
      @RequestParam(value = OU, required = false) Dn ou,
      @RequestParam(value = SCOPE, required = false) SearchScope searchScope,
      ModelMap model) {

    return Optional.ofNullable(computerName)
        .flatMap(name -> domainComputerService.getComputer(name, ou, searchScope))
        .map(computer -> {
          DomainComputerEditRequest req = new DomainComputerEditRequest(
              computer, getProperties().getParentDn(computer.getDistinguishedName()));
          model.addAttribute("computerEditRequest", req);
          List<DomainGroup> groups = domainGroupService
              .getMemberships(computerName, ou, searchScope)
              .toList();
          model.addAttribute("groups", groups);
          return "admin/computer-edit";
        })
        .orElseGet(() -> entityNotFoundRedirect(model, "Computer", "todo", computerName,
            "computers"));
  }

  @PostMapping(path = "/admin/computer-edit")
  public String updateComputer(
      @ModelAttribute(name = OU, binding = false) Dn ou,
      @ModelAttribute(name = SCOPE, binding = false) SearchScope scope,
      @ModelAttribute(name = "computerEditRequest") DomainComputerEditRequest computerEditRequest,
      ModelMap model,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {

    getLogger().debug("updateComputer({})", computerEditRequest);

    DomainComputer updatedComputer = updateComputer(bindingResult, computerEditRequest);

    if (bindingResult.hasErrors()) {
      getLogger().debug("Updating computer failed. Some fields were invalid.");
      List<DomainGroup> groups = domainGroupService
          .getMemberships(computerEditRequest.getComputer().getSamAccountName(), ou, scope)
          .toList();
      model.addAttribute("groups", groups);
      return "admin/computer-edit";
    }

    model.clear();
    String msg = String.format("Computer '%s' was successfully updated.",
        updatedComputer.getName());
    RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.SUCCESS, msg,
        "i18n.computer.edited", updatedComputer.getName());
    redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);

    Map<String, Object> parameters = getParamterMap(computerEditRequest.getNewOuDn());
    String redirect = getRedirectUri("computer-edit?name={{computer.samAccountName}}",
        PAGE_AND_OU_PARAMS, putToParameterMap(parameters, "computer", updatedComputer));
    logRedirectTo("Computer successfully updated.", redirect);
    return redirect;
  }

  private DomainComputer updateComputer(BindingResult bindingResult,
      DomainComputerEditRequest computerEditRequest) {
    DomainComputer computer = computerEditRequest.getComputer();
    Dn ou = computerEditRequest.getNewOuDn();
    try {
      Dn parentDn = getProperties().getParentDn(computer.getDistinguishedName());
      Dn ouDn = getProperties().getBaseDn(ou);
      Dn newOu = parentDn.isSame(ouDn) ? null : ouDn;
      return domainComputerService.updateComputer(computer, newOu);

    } catch (ServiceException e) {
      handleException(bindingResult, e);
    }
    return computer;
  }

  private void handleException(BindingResult bindingResult, ServiceException serviceException) {

    getLogger().debug("Handle exception of bind target '{}'",
        bindingResult.getTarget(), serviceException);

    if (!(bindingResult.getTarget() instanceof DomainComputerEditRequest)) {
      return;
    }

    String errorCode = Objects.requireNonNullElse(serviceException.getErrorCode(), "");
    switch (errorCode) {
      case EC_SAM_ACCOUNT_NAME_REQUIRED: {
        bindingResult.rejectValue("computer.samAccountName", "code",
            "Computer name is required.");
        break;
      }
      case EC_ILLEGAL_SAM_ACCOUNT_NAME: {
        bindingResult.rejectValue("computer.samAccountName", "code",
            "Computer name contains illegal characters.");
        break;
      }
      case EC_SAM_ACCOUNT_ALREADY_EXISTS: {
        bindingResult.rejectValue("computer.samAccountName", "code",
            "Computer name already exists.");
        break;
      }
      case EC_DN_ALREADY_EXISTS: {
        bindingResult.rejectValue("computer.samAccountName", "code",
            "Distinguished name already exists.");
        break;
      }
      case EC_EMPTY_OU_RDN: {
        bindingResult.rejectValue("newOu", "code",
            "Organizational unit is empty.");
        break;
      }
      case EC_OU_NOT_FOUND: {
        bindingResult.rejectValue("newOu", "code",
            "Organizational unit was not found.");
        break;
      }
      default: {
        getLogger().error("Editing computer failed with a not mapped exception.", serviceException);
        throw serviceException;
      }
    }
  }
}
