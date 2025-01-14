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
import org.bremersee.dccon.controller.ui.components.OrganizationalUnitComponent;
import org.bremersee.dccon.controller.ui.components.PageableComponent;
import org.bremersee.dccon.controller.ui.model.RedirectMessage;
import org.bremersee.dccon.controller.ui.model.RedirectMessageType;
import org.bremersee.dccon.model.TreeSearchScope;
import org.bremersee.dccon.service.DomainComputerService;
import org.ldaptive.dn.Dn;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The type ComputersController.
 *
 * @author Christian Bremer
 */
@Controller
public class ComputerEditMiscController extends AbstractEditController implements PageableComponent,
    OrganizationalUnitComponent {

  private final DomainComputerService domainComputerService;

  public ComputerEditMiscController(
      DomainControllerProperties domainControllerProperties,
      LocaleResolver localeResolver,
      DomainComputerService domainComputerService) {
    super(domainControllerProperties, localeResolver);
    this.domainComputerService = domainComputerService;
  }

  @Override
  public String getDefaultSort() {
    return USER_SORT;
  }

  @GetMapping(path = "/admin/computer-edit-misc")
  public String displayComputerDelete(
      @RequestParam(value = "name", required = false) String computerName,
      @RequestParam(value = OU, required = false) Dn ou,
      @RequestParam(value = SCOPE, required = false) TreeSearchScope searchScope,
      ModelMap model) {

    return Optional.ofNullable(computerName)
        .flatMap(name -> domainComputerService.getComputer(computerName, ou, searchScope))
        .map(computer -> {
          model.addAttribute("computer", computer);
          return "admin/computer-edit-misc";
        })
        .orElseGet(() -> entityNotFoundRedirect(model, "Computer", "todo", computerName, "computers"));
  }

  @PostMapping(path = "/admin/computer-edit-misc-delete-computer")
  public String deleteComputer(
      @RequestParam(value = "name", required = false) String computerName,
      @RequestParam(value = "verificationName", required = false) String verificationName,
      RedirectAttributes redirectAttributes) {

    getLogger().debug("deleteComputer({}, {})", computerName, verificationName);

    Map<String, Object> parameters = getParamterMap();

    if (isEmpty(computerName)) {
      String defaultMsg = "Deleting computer failed. Computer name is required.";
      RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.DANGER, defaultMsg, "todo");
      redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);
      String redirect = getRedirectUri("computers", PAGE_AND_OU_PARAMS, parameters);
      logRedirectTo(defaultMsg, redirect);
      return redirect;
    }

    Map<String, Object> parametersWithComputerName = putToParameterMap(parameters,
        "computerName", computerName);

    if (!computerName.equalsIgnoreCase(verificationName)) {
      String defaultMsg = "Deleting computer failed. The given computer name doesn't match.";
      RedirectMessage dmsg = getRedirectMessage(RedirectMessageType.DANGER, defaultMsg, "todo");
      redirectAttributes.addFlashAttribute("dmsg", dmsg);
      String redirect = getRedirectUri("computer-edit-misc?name={{computerName}}",
          PAGE_AND_OU_PARAMS, parametersWithComputerName);
      logRedirectTo(defaultMsg, redirect);
      return redirect;
    }

    if (!domainComputerService.deleteComputer(computerName)) {
      String defaultMsg = "Deleting computer failed. It is still present.";
      RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.WARNING, defaultMsg, "todo");
      redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);
      String redirect = getRedirectUri("computer-edit-misc?name={{computerName}}",
          PAGE_AND_OU_PARAMS, parametersWithComputerName);
      logRedirectTo(defaultMsg, redirect);
      return redirect;
    }

    String defaultMsg = "Computer was successfully deleted.";
    RedirectMessage rmsg = getRedirectMessage(RedirectMessageType.SUCCESS, defaultMsg, "todo");
    redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);
    String redirect = getRedirectUri("computers",
        PAGE_AND_OU_PARAMS, parametersWithComputerName);
    logRedirectTo(defaultMsg, redirect);
    return redirect;
  }

}
