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

import static java.util.Objects.requireNonNullElse;

import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.components.OrganisationalUnitsComponent;
import org.bremersee.dccon.controller.ui.components.OrganizationalUnitComponent;
import org.bremersee.dccon.controller.ui.components.PageableComponent;
import org.bremersee.dccon.controller.ui.model.DomainGroupEditRequest;
import org.bremersee.dccon.controller.ui.model.RedirectMessage;
import org.bremersee.dccon.controller.ui.model.RedirectMessageType;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.model.TreeSearchScope;
import org.bremersee.dccon.service.DomainGroupService;
import org.bremersee.dccon.service.DomainService;
import org.bremersee.dccon.service.OrganizationalUnitService;
import org.bremersee.exception.ServiceException;
import org.ldaptive.dn.Dn;
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
 * The type UsersController.
 *
 * @author Christian Bremer
 */
@Controller
public class GroupEditController extends AbstractEditController implements PageableComponent,
    OrganizationalUnitComponent, OrganisationalUnitsComponent {

  private final DomainService domainService;

  private final DomainGroupService domainGroupService;

  @Getter
  private final OrganizationalUnitService organizationalUnitService;

  public GroupEditController(
      DomainControllerProperties domainControllerProperties,
      LocaleResolver localeResolver,
      DomainService domainService,
      DomainGroupService domainGroupService,
      OrganizationalUnitService organizationalUnitService) {
    super(domainControllerProperties, localeResolver);
    this.domainService = domainService;
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

  @GetMapping(path = "/admin/group-edit")
  public String displayGroupEdit(
      @RequestParam(value = "name", required = false) String groupName,
      @RequestParam(value = OU, required = false) Dn ou,
      @RequestParam(value = SCOPE, required = false) TreeSearchScope searchScope,
      ModelMap model,
      RedirectAttributes redirectAttributes) {

    return Optional.ofNullable(groupName)
        .flatMap(name -> domainGroupService.getGroup(name, ou, searchScope))
        .map(group -> {
          model.addAttribute("group", group);
          DomainGroupEditRequest req = DomainGroupEditRequest.MAPPER.map(group);
          model.addAttribute("groupEditRequest", req);
          return "admin/group-edit";
        })
        .orElseGet(() -> entityNotFoundRedirect(
            redirectAttributes, "Group", "todo", groupName, "groups"));
  }

  @PostMapping(path = "/admin/group-edit")
  public String updateGroup(
      @RequestParam(value = "name", required = false) String oldSamAccountName,
      @RequestParam(value = OU, required = false) Dn ou,
      @RequestParam(value = SCOPE, required = false) TreeSearchScope searchScope,
      @ModelAttribute(name = "groupEditRequest") DomainGroupEditRequest groupEditRequest,
      ModelMap model,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {

    getLogger().debug("updateGroup({}, {})", oldSamAccountName, groupEditRequest);

    return Optional.ofNullable(oldSamAccountName)
        .or(() -> Optional.ofNullable(groupEditRequest.getSamAccountName()))
        .flatMap(oldName -> domainGroupService.getGroup(oldName, ou, searchScope))
        .map(existingGroup -> updateGroup(
            existingGroup, groupEditRequest, model, bindingResult, redirectAttributes))
        .orElseGet(() -> entityNotFoundRedirect(
            redirectAttributes, "Group", "todo", oldSamAccountName, "groups"));
  }

  private String updateGroup(
      DomainGroup existingGroup,
      DomainGroupEditRequest groupEditRequest,
      ModelMap model,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {

    String oldSamAccountName = existingGroup.getSamAccountName();
    DomainGroupEditRequest.MAPPER.update(existingGroup, groupEditRequest);
    Dn ou = groupEditRequest.getNewOuDn();
    try {
      Dn parentDn = existingGroup.getDn().getParent();
      Dn ouDn = getProperties().getBaseDn(ou);
      Dn newOu = parentDn.isSame(ouDn) ? null : ouDn;
      DomainGroup updatedGroup = domainGroupService
          .updateGroup(oldSamAccountName, existingGroup, newOu);

      model.clear();
      String msg = String.format("Group '%s' was successfully updated.", updatedGroup.getName());
      RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.SUCCESS, msg,
          "i18n.group.edited", updatedGroup.getName());
      redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);

      Map<String, Object> parameters = getParamterMap(updatedGroup.getDn().getParent());
      String redirect = getRedirectUri("group-edit?name={{group.samAccountName}}",
          PAGE_AND_OU_PARAMS, putToParameterMap(parameters, "group", updatedGroup));
      logRedirectTo("Group successfully updated.", redirect);
      return redirect;

    } catch (ServiceException e) {
      handleException(bindingResult, e);
      existingGroup.setSamAccountName(oldSamAccountName);
      model.addAttribute("group", existingGroup);
      return "admin/group-edit";
    }
  }

  private void handleException(BindingResult bindingResult, ServiceException serviceException) {

    Object bindTarget = bindingResult.getTarget();
    getLogger().debug("handleException of bind target '{}'", bindTarget, serviceException);
    Assert.isTrue(bindTarget instanceof DomainGroupEditRequest, "Illegal bind target.");
    String errorCode = requireNonNullElse(serviceException.getErrorCode(), "");
    switch (errorCode) {
      case EC_SAM_ACCOUNT_NAME_REQUIRED: {
        bindingResult.rejectValue("samAccountName", "code",
            "Group name is required.");
        break;
      }
      case EC_ILLEGAL_SAM_ACCOUNT_NAME: {
        bindingResult.rejectValue("samAccountName", "code",
            "Group name contains illegal characters.");
        break;
      }
      case EC_SAM_ACCOUNT_ALREADY_EXISTS: {
        bindingResult.rejectValue("samAccountName", "code",
            "Group name already exists.");
        break;
      }
      case EC_GID_NUMBER_ALREADY_EXISTS: {
        bindingResult.rejectValue("gidNumber", "code",
            "Unix GID number already exists.");
        break;
      }
      case EC_DN_ALREADY_EXISTS: {
        bindingResult.rejectValue("samAccountName", "code",
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
        getLogger().error("Editing group failed with a not mapped exception.", serviceException);
        throw serviceException;
      }
    }
  }
}
