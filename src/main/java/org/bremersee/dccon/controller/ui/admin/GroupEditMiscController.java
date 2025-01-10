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

import java.util.Map;
import java.util.Optional;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.AbstractController;
import org.bremersee.dccon.controller.ui.components.OrganizationalUnitComponent;
import org.bremersee.dccon.controller.ui.components.PageableComponent;
import org.bremersee.dccon.controller.ui.components.RedirectComponent;
import org.bremersee.dccon.controller.ui.model.RedirectMessage;
import org.bremersee.dccon.controller.ui.model.RedirectMessageType;
import org.bremersee.dccon.service.DomainService;
import org.bremersee.dccon.service.DomainGroupService;
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
 * The type GroupsController.
 *
 * @author Christian Bremer
 */
@Controller
public class GroupEditMiscController extends AbstractController implements PageableComponent,
    RedirectComponent, OrganizationalUnitComponent {

  private final DomainService domainService;

  private final DomainGroupService domainGroupService;

  public GroupEditMiscController(
      DomainControllerProperties domainControllerProperties,
      LocaleResolver localeResolver,
      DomainService domainService,
      DomainGroupService domainGroupService) {
    super(domainControllerProperties, localeResolver);
    this.domainGroupService = domainGroupService;
    this.domainService = domainService;
  }

  @Override
  public String getDefaultSort() {
    return USER_SORT;
  }

  @ModelAttribute("passwordPattern")
  public String getPasswordPattern() {
    return domainService.getPasswordInformation().getPasswordRegex();
  }

  @GetMapping(path = "/admin/group-edit-misc")
  public String displayGroupEditDangerZone(
      @RequestParam(value = "name", required = false) String groupName,
      @RequestParam(value = OU, required = false) Dn ou,
      @RequestParam(value = SCOPE, required = false) SearchScope searchScope,
      ModelMap model) {

    return Optional.ofNullable(groupName)
        .flatMap(name -> domainGroupService.getGroup(groupName, ou, searchScope))
        .map(group -> {
          model.addAttribute("group", group);
          return "admin/group-edit-misc";
        })
        .orElseGet(() -> {
          String msg = String.format("Group '%s' not found.", groupName);
          model.addAttribute("rmsg", new RedirectMessage(msg, RedirectMessageType.WARNING));
          return "admin/group-edit-misc";
        });
  }

  @PostMapping(path = "/admin/group-edit-misc-delete-group")
  public String deleteGroup(
      @RequestParam(value = PAGE, required = false) Integer page,
      @RequestParam(value = SIZE, required = false) Integer size,
      @RequestParam(value = SORT, required = false) String sort,
      @RequestParam(value = QUERY, required = false) String query,
      @RequestParam(value = OU, required = false) Dn ou,
      @RequestParam(value = SCOPE, required = false) SearchScope scope,
      @RequestParam(value = "name", required = false) String groupName,
      @RequestParam(value = "verificationName", required = false) String verificationName,
      RedirectAttributes redirectAttributes) {

    getLogger().debug("deleteGroup({}, {}, {}, {}, {}, {}, {})",
        page, size, sort, query, scope, groupName, verificationName);
    Map<String, Object> parameters = getParamterMap(page, size, sort, query, ou, scope);

    if (isEmpty(groupName)) {
      String defaultMsg = "Deleting group failed. Group name is required.";
      RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.DANGER, defaultMsg, "todo");
      redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);
      String redirect = getRedirectUri("groups", PAGE_AND_OU_PARAMS, parameters);
      logRedirectTo(defaultMsg, redirect);
      return redirect;
    }

    Map<String, Object> parametersWithGroupname = addToMap(parameters, "groupName", groupName);

    if (!groupName.equalsIgnoreCase(verificationName)) {
      String defaultMsg = "Deleting group failed. The given group name doesn't match.";
      RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.DANGER, defaultMsg, "todo");
      redirectAttributes.addFlashAttribute("dmsg", rmsg);
      String redirect = getRedirectUri("group-edit-misc?name={{groupName}}",
          PAGE_AND_OU_PARAMS, parametersWithGroupname);
      logRedirectTo(defaultMsg, redirect);
      return redirect;
    }

    if (!domainGroupService.deleteGroup(groupName)) {
      String defaultMsg = "Deleting group failed. It is still present.";
      RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.WARNING, defaultMsg, "todo");
      redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);
      String redirect = getRedirectUri("group-edit-misc?name={{groupName}}",
          PAGE_AND_OU_PARAMS, parametersWithGroupname);
      logRedirectTo(defaultMsg, redirect);
      return redirect;
    }

    String defaultMsg = "Group was successfully deleted.";
    RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.SUCCESS, defaultMsg, "todo");
    redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);
    String redirect = getRedirectUri("groups",
        PAGE_AND_OU_PARAMS, parametersWithGroupname);
    logRedirectTo(defaultMsg, redirect);
    return redirect;
  }

}
