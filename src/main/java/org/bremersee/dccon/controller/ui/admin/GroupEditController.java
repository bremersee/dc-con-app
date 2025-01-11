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

import java.util.Map;
import java.util.Objects;
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
      @RequestParam(value = SCOPE, required = false) SearchScope searchScope,
      ModelMap model) {

    return Optional.ofNullable(groupName)
        .flatMap(name -> domainGroupService.getGroup(name, ou, searchScope))
        .map(group -> {
          DomainGroupEditRequest req = new DomainGroupEditRequest(
              group, getProperties().getParentDn(group.getDistinguishedName()));
          model.addAttribute("groupEditRequest", req);
          return "admin/group-edit";
        })
        .orElseGet(() -> entityNotFoundRedirect(model, "Group", "todo", groupName, "admin/groups"));
  }

  @PostMapping(path = "/admin/group-edit")
  public String updateGroup(
      @ModelAttribute(name = "groupEditRequest") DomainGroupEditRequest groupEditRequest,
      ModelMap model,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {

    getLogger().debug("updateGroup({})", groupEditRequest);

    DomainGroup updatedGroup = updateGroup(bindingResult, groupEditRequest);

    if (bindingResult.hasErrors()) {
      getLogger().debug("Updating group failed. Some fields were invalid.");
      return "admin/group-edit";
    }

    model.clear();
    String msg = String.format("Group '%s' was successfully updated.", updatedGroup.getName());
    RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.SUCCESS, msg,
        "i18n.group.edited", updatedGroup.getGroupType());
    redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);

    Map<String, Object> parameters = getParamterMap(groupEditRequest.getNewOuDn());
    String redirect = getRedirectUri("group-edit?name={{group.samAccountName}}",
        PAGE_AND_OU_PARAMS, putToParameterMap(parameters, "group", updatedGroup));
    logRedirectTo("Group successfully updated.", redirect);
    return redirect;
  }

  private DomainGroup updateGroup(BindingResult bindingResult,
      DomainGroupEditRequest groupEditRequest) {
    String groupName = groupEditRequest.getOldSamAccountName();
    DomainGroup group = groupEditRequest.getGroup();
    Dn ou = groupEditRequest.getNewOuDn();
    try {
      Dn parentDn = getProperties().getParentDn(group.getDistinguishedName());
      Dn ouDn = getProperties().getBaseDn(ou);
      Dn newOu = parentDn.isSame(ouDn) ? null : ouDn;
      return domainGroupService.updateGroup(groupName, group, newOu);

    } catch (ServiceException e) {
      handleException(bindingResult, e);
    }
    return group;
  }

  private void handleException(BindingResult bindingResult, ServiceException serviceException) {

    getLogger().debug("Handle exception of bind target '{}'",
        bindingResult.getTarget(), serviceException);

    if (!(bindingResult.getTarget() instanceof DomainGroupEditRequest)) {
      return;
    }

    String errorCode = Objects.requireNonNullElse(serviceException.getErrorCode(), "");
    switch (errorCode) {
      case EC_SAM_ACCOUNT_NAME_REQUIRED: {
        bindingResult.rejectValue("group.samAccountName", "code",
            "Group name is required.");
        break;
      }
      case EC_ILLEGAL_SAM_ACCOUNT_NAME: {
        bindingResult.rejectValue("group.samAccountName", "code",
            "Group name contains illegal characters.");
        break;
      }
      case EC_SAM_ACCOUNT_ALREADY_EXISTS: {
        bindingResult.rejectValue("group.samAccountName", "code",
            "Group name already exists.");
        break;
      }
      case EC_GID_NUMBER_ALREADY_EXISTS: {
        bindingResult.rejectValue("group.gidNumber", "code",
            "Unix GID number already exists.");
        break;
      }
      case EC_DN_ALREADY_EXISTS: {
        bindingResult.rejectValue("group.samAccountName", "code",
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
      case EC_UPDATING_GROUP_FAILED: { // TODO global
        getLogger().error("Editing group failed.", serviceException);
        bindingResult.rejectValue("group.samAccountName", "code",
            "Something went wrong. Please try again later.");
        break;
      }
      default: {
        getLogger().error("Editing group failed with a not mapped exception.", serviceException);
        throw serviceException;
      }
    }
  }
}
