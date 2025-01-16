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
import java.util.Optional;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.components.OrganizationalUnitComponent;
import org.bremersee.dccon.controller.ui.components.PageableComponent;
import org.bremersee.dccon.controller.ui.model.RedirectMessage;
import org.bremersee.dccon.controller.ui.model.RedirectMessageType;
import org.bremersee.dccon.controller.ui.model.SamAccountDeleteRequest;
import org.bremersee.dccon.model.DomainComputer;
import org.bremersee.dccon.model.TreeSearchScope;
import org.bremersee.dccon.service.DomainComputerService;
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
 * The type ComputersController.
 *
 * @author Christian Bremer
 */
@Controller
public class ComputerDeleteController extends AbstractEditController implements PageableComponent,
    OrganizationalUnitComponent {

  private final DomainComputerService domainComputerService;

  public ComputerDeleteController(
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

  @GetMapping(path = "/admin/computer-delete")
  public String displayComputerDelete(
      @RequestParam(value = "name", required = false) String computerName,
      @RequestParam(value = OU, required = false) Dn ou,
      @RequestParam(value = SCOPE, required = false) TreeSearchScope searchScope,
      ModelMap model,
      RedirectAttributes redirectAttributes) {

    return Optional.ofNullable(computerName)
        .flatMap(name -> domainComputerService.getComputer(computerName, ou, searchScope))
        .map(computer -> {
          model.addAttribute("computer", computer);
          model.addAttribute("deleteRequest", new SamAccountDeleteRequest(computer));
          return "admin/computer-delete";
        })
        .orElseGet(() -> entityNotFoundRedirect(
            redirectAttributes, "Computer", "todo", computerName, "computers"));
  }

  @PostMapping(path = "/admin/computer-delete")
  public String deleteComputer(
      @RequestParam(value = OU, required = false) Dn ou,
      @RequestParam(value = SCOPE, required = false) TreeSearchScope searchScope,
      @ModelAttribute("deleteRequest") SamAccountDeleteRequest deleteRequest,
      ModelMap model,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {

    getLogger().debug("deleteComputer({})", deleteRequest);
    return Optional.ofNullable(deleteRequest.getSamAccountName())
        .flatMap(name -> domainComputerService.getComputer(name, ou, searchScope))
        .map(computer -> {
          if (!computer.getName().equalsIgnoreCase(deleteRequest.getVerificationName())) {
            bindingResult.rejectValue("verificationName", "todo", "The name doesn't match.");
            model.addAttribute("computer", computer);
            return "admin/computer-delete";
          }
          return deleteComputer(computer, model, redirectAttributes);
        })
        .orElseGet(() -> entityNotFoundRedirect(
            redirectAttributes, "Computer", "todo", deleteRequest.getSamAccountName(),
            "computers"));
  }

  private String deleteComputer(
      DomainComputer computer,
      ModelMap model,
      RedirectAttributes redirectAttributes) {

    boolean result = domainComputerService.deleteComputer(computer.getSamAccountName());
    model.clear();
    RedirectMessage rmsg;
    if (result) {
      rmsg = getRedirectMessage(RedirectMessageType.SUCCESS,
          String.format("Computer '%s' was successfully deleted.", computer.getName()),
          "todo", computer.getName());
    } else {
      rmsg = getRedirectMessage(RedirectMessageType.WARNING,
          String.format("Somehow the computer '%s' was not deleted.", computer.getName()),
          "todo", computer.getName());
    }
    redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);

    Map<String, Object> parameters = getParamterMap();
    String redirect = getRedirectUri("computers", PAGE_AND_OU_PARAMS, parameters);
    logRedirectTo("Computer deletion message.", redirect);
    return redirect;
  }

}
