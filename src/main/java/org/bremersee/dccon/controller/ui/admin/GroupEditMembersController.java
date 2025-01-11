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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import lombok.Getter;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.components.OrganisationalUnitsComponent;
import org.bremersee.dccon.controller.ui.components.OrganizationalUnitComponent;
import org.bremersee.dccon.controller.ui.components.PageableComponent;
import org.bremersee.dccon.controller.ui.model.RedirectMessage;
import org.bremersee.dccon.controller.ui.model.RedirectMessageType;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.model.DomainGroupMembers;
import org.bremersee.dccon.service.DomainGroupService;
import org.bremersee.dccon.service.DomainService;
import org.bremersee.dccon.service.OrganizationalUnitService;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
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
public class GroupEditMembersController extends AbstractEditController implements PageableComponent,
    OrganizationalUnitComponent, OrganisationalUnitsComponent {

  private final DomainService domainService;

  private final DomainGroupService domainGroupService;

  @Getter
  private final OrganizationalUnitService organizationalUnitService;

  public GroupEditMembersController(
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

  @ModelAttribute("possibleMembers")
  public DomainGroupMembers addMemberSelectOptions(
      @RequestParam(value = "name", required = false) String groupName,
      @RequestParam(value = OU, required = false) Dn ou,
      @RequestParam(value = SCOPE, required = false) SearchScope searchScope) {
    return Optional.ofNullable(groupName)
        .map(name -> domainGroupService.findPossibleMembers(groupName, ou, searchScope))
        .orElseGet(DomainGroupMembers::empty);
  }

  @GetMapping(path = "/admin/group-edit-members")
  public String displayGroupEditMembers(
      @RequestParam(value = "name", required = false) String groupName,
      @RequestParam(value = OU, required = false) Dn ou,
      @RequestParam(value = SCOPE, required = false) SearchScope searchScope,
      ModelMap model) {

    return Optional.ofNullable(groupName)
        .flatMap(name -> domainGroupService.getGroup(name, ou, searchScope))
        .map(group -> {
          group.setMembers(group.getMembers().stream()
              .map(dn -> Base64.getEncoder().encodeToString(dn.getBytes(StandardCharsets.UTF_8)))
              .toList());
          model.addAttribute("group", group);
          return "admin/group-edit-members";
        })
        .orElseGet(() -> entityNotFoundRedirect(model, "Group", "todo", groupName, "admin/groups"));
  }

  @PostMapping(path = "/admin/group-edit-members")
  public String updateGroupMembers(
      @ModelAttribute(name = "group") DomainGroup group,
      ModelMap model,
      RedirectAttributes redirectAttributes) {

    getLogger().debug("updateGroupMembers({})", group.getSamAccountName());

    group.setMembers(group.getMembers().stream()
        .map(base64 -> new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8))
        .toList());
    domainGroupService.updateGroup(group.getSamAccountName(), group, null);

    model.clear();
    String msg = String.format("Members of group '%s' were successfully updated.", group.getName());
    RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.SUCCESS, msg,
        "todo", group.getName());
    redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);

    String redirect = getRedirectUri("group-edit-members?name={{group.samAccountName}}",
        PAGE_AND_OU_PARAMS, putToParameterMap(getParamterMap(), "group", group));
    logRedirectTo("Members of group successfully updated.", redirect);
    return redirect;
  }

}
