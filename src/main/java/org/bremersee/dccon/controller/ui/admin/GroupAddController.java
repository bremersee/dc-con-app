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

import static org.springframework.util.ObjectUtils.isEmpty;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.Getter;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.AbstractController;
import org.bremersee.dccon.controller.ui.components.DomainGroupTypesComponent;
import org.bremersee.dccon.controller.ui.components.OrganisationalUnitsComponent;
import org.bremersee.dccon.controller.ui.components.OrganizationalUnitComponent;
import org.bremersee.dccon.controller.ui.components.PageableComponent;
import org.bremersee.dccon.controller.ui.components.RedirectComponent;
import org.bremersee.dccon.controller.ui.model.DomainGroupAddRequest;
import org.bremersee.dccon.controller.ui.model.RedirectMessage;
import org.bremersee.dccon.controller.ui.model.RedirectMessageType;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.model.DomainGroupType;
import org.bremersee.dccon.model.DomainGroupType.Purpose;
import org.bremersee.dccon.model.DomainGroupType.Scope;
import org.bremersee.dccon.model.DomainGroupTypeContainer;
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
 * The group add controller.
 *
 * @author Christian Bremer
 */
@Controller
public class GroupAddController extends AbstractController implements PageableComponent,
    RedirectComponent, DomainGroupTypesComponent, OrganizationalUnitComponent,
    OrganisationalUnitsComponent {

  private final DomainService domainService;

  private final DomainGroupService domainGroupService;

  @Getter
  private final OrganizationalUnitService organizationalUnitService;

  public GroupAddController(
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

  @GetMapping(path = "/admin/group-add")
  public String displayGroupAdd(
      @RequestParam(name = OU, required = false) Dn ou,
      ModelMap model) {

    getLogger().debug("displayGroupAdd({})", ou);
    Dn ouDn = Optional.ofNullable(ou)
        .filter(dn -> !dn.isEmpty())
        .filter(dn -> !dn.isSame(getProperties().getBaseDn()))
        .orElseGet(() -> getProperties().getBaseDn(getProperties().getGroup().getDefaultGroupOu()));
    DomainGroupAddRequest groupAddRequest = new DomainGroupAddRequest(
        new DomainGroup(), ouDn.format());
    model.addAttribute("groupAddRequest", groupAddRequest);
    return "admin/group-add";
  }

  @PostMapping(path = "/admin/group-add")
  public String addGroup(
      @ModelAttribute(name = PAGE, binding = false) Integer page,
      @ModelAttribute(name = SIZE, binding = false) Integer size,
      @ModelAttribute(name = SORT, binding = false) String sort,
      @ModelAttribute(name = QUERY, binding = false) String query,
      @ModelAttribute(name = SCOPE, binding = false) SearchScope scope,
      @ModelAttribute(name = "groupAddRequest") DomainGroupAddRequest groupAddRequest,
      ModelMap model,
      HttpServletRequest request,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {

    getLogger().debug("addGroup({}, {}, {}, {}, {}, {})",
        groupAddRequest, page, size, sort, query, scope);
    Map<String, Object> parameters = Map.of(
        PAGE, Optional.ofNullable(page).orElse(PAGE_DEFAULT_INT),
        SIZE, Optional.ofNullable(size)
            .filter(s -> s > 0)
            .orElse(SIZE_DEFAULT_INT),
        SORT, Optional.ofNullable(sort).orElse(USER_SORT),
        QUERY, Optional.ofNullable(query).orElse(""),
        OU, Optional.ofNullable(groupAddRequest)
            .map(DomainGroupAddRequest::getNewOuDn)
            .map(Dn::format)
            .orElse(getProperties().getGroup().getDefaultGroupOu().format()),
        SCOPE, Optional.ofNullable(scope).orElse(SearchScope.ONELEVEL)
    );

    getLogger().debug("Try to add group '{}'.", groupAddRequest);

    if (isEmpty(groupAddRequest)) {
      String redirect = getRedirectUri("/admin/groups", PAGE_AND_OU_PARAMS, parameters);
      getLogger().debug("Group add request is empty. Redirecting to {}", redirect);
      return redirect;
    }

    DomainGroupType groupType = DomainGroupType.fromScopeAndPurpose(
        Scope.fromString(groupAddRequest.getGroupScope()),
        Purpose.fromString(groupAddRequest.getGroupPurpose()));
    groupAddRequest.getGroup().setGroupType(new DomainGroupTypeContainer(groupType));
    DomainGroup addedGroup = addGroup(bindingResult, groupAddRequest);

    if (bindingResult.hasErrors()) {
      getLogger().debug("Adding group failed. Some fields were invalid.");
      return "admin/group-add";
    }

    model.clear();
    String msg = getMessageSource().getMessage(
        "i18n.group.added",
        new Object[]{addedGroup.getSamAccountName()},
        String.format("Group '%s' was successfully added.", addedGroup.getSamAccountName()),
        resolveLocale(request));
    RedirectMessage rmsg = new RedirectMessage(msg, RedirectMessageType.SUCCESS);
    redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);

    parameters = new HashMap<>(parameters);
    parameters.put("group", addedGroup);
    String redirect = getRedirectUri("group-edit?name={{group.samAccountName}}",
        PAGE_AND_OU_PARAMS, parameters);
    getLogger().debug("Group successfully added. Redirecting to {}", redirect);
    return redirect;
  }

  private DomainGroup addGroup(BindingResult bindingResult, DomainGroupAddRequest groupAddRequest) {
    if (isEmpty(groupAddRequest.getGroupScope())) {
      bindingResult.rejectValue("groupScope", "code",
          "Group scope is required.");
    }
    if (isEmpty(groupAddRequest.getGroupPurpose())) {
      bindingResult.rejectValue("groupPurpose", "code",
          "Group type is required.");
    }
    if (bindingResult.hasErrors()) {
      return groupAddRequest.getGroup();
    }

    Scope groupScope = groupAddRequest.getSelectedGroupScope();
    Purpose groupPurpose = groupAddRequest.getSelectedGroupPurpose();
    DomainGroupType groupType = DomainGroupType.fromScopeAndPurpose(groupScope, groupPurpose);
    DomainGroup group = groupAddRequest.getGroup();
    group.setGroupType(new DomainGroupTypeContainer(groupType));
    Dn ou = Optional.ofNullable(groupAddRequest.getNewOu())
        .map(Dn::new)
        .orElseGet(() -> getProperties().getGroup().getDefaultGroupOu());
    try {
      return domainGroupService.addGroup(group, ou);

    } catch (ServiceException e) {
      handleException(bindingResult, e);
    }
    return group;
  }

  private void handleException(BindingResult bindingResult, ServiceException serviceException) {

    Object bindTarget = bindingResult.getTarget();
    getLogger().debug("handleException of bind target '{}'", bindTarget, serviceException);

    if (!(bindTarget instanceof DomainGroupAddRequest)) {
      return;
    }

    String errorCode = Objects.requireNonNullElse(serviceException.getErrorCode(), "");
    switch (errorCode) {
      case EC_SAM_ACCOUNT_NAME_REQUIRED: {
        bindingResult.rejectValue("group.samAccountName", "code",
            "Group name is required.");
        break;
      }
      case EC_SAM_ACCOUNT_ALREADY_EXISTS: {
        bindingResult.rejectValue("group.samAccountName", "code",
            "Group name already exists.");
        break;
      }
      case EC_EMPTY_OU_RDN: {
        bindingResult.rejectValue("ou", "code",
            "Organizational unit is empty.");
        break;
      }
      case EC_OU_NOT_FOUND: {
        bindingResult.rejectValue("ou", "code",
            "Organizational unit was not found.");
        break;
      }
      case EC_ADDING_GROUP_FAILED: { // TODO global
        getLogger().error("Adding group failed.", serviceException);
        bindingResult.rejectValue("group.samAccountName", "code",
            "Something went wrong. Please try again later.");
        break;
      }
      default: {
        getLogger().error("Adding group failed with a not mapped exception.", serviceException);
        throw serviceException;
      }
    }
  }

}
