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

import java.util.Map;
import java.util.Optional;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.components.PageableComponent;
import org.bremersee.dccon.controller.ui.components.RedirectComponent;
import org.bremersee.dccon.controller.ui.model.OrganizationalUnitDeleteRequest;
import org.bremersee.dccon.controller.ui.model.RedirectMessage;
import org.bremersee.dccon.controller.ui.model.RedirectMessageType;
import org.bremersee.dccon.service.OrganizationalUnitService;
import org.ldaptive.dn.Dn;
import org.ldaptive.dn.NameValue;
import org.ldaptive.dn.RDn;
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
 * The type OrganizationalUnitAddController.
 *
 * @author Christian Bremer
 */
@Controller
public class OrganizationalUnitDeleteController extends AbstractEditController
    implements PageableComponent, RedirectComponent {

  private final OrganizationalUnitService organizationalUnitService;

  @Override
  public String getDefaultSort() {
    return OU_SORT;
  }

  public OrganizationalUnitDeleteController(
      DomainControllerProperties properties,
      LocaleResolver localeResolver,
      OrganizationalUnitService organizationalUnitService) {
    super(properties, localeResolver);
    this.organizationalUnitService = organizationalUnitService;
  }

  @GetMapping(path = "/admin/organizational-unit-delete")
  public String displayOrganizationalUnitDelete(
      @RequestParam(value = "name", required = false) Dn ouDn,
      ModelMap model,
      RedirectAttributes redirectAttributes) {
    getLogger().debug("displayOrganizationalUnitDelete({})", ouDn);
    String name = Optional.ofNullable(ouDn)
        .map(Dn::getRDn)
        .map(RDn::getNameValue)
        .map(NameValue::getStringValue)
        .orElse("null");
    return Optional.ofNullable(ouDn)
        .filter(dn -> !dn.isEmpty())
        .flatMap(organizationalUnitService::getOrganizationalUnit)
        .map(ou -> {
          boolean hasChildren = organizationalUnitService
              .hasChildren(new Dn(ou.getDistinguishedName()));
          OrganizationalUnitDeleteRequest ouDeleteRequest = new OrganizationalUnitDeleteRequest(
              ou, hasChildren);
          model.put("ouDeleteRequest", ouDeleteRequest);
          return "admin/organizational-unit-delete";
        })
        .orElseGet(() -> entityNotFoundRedirect(
            redirectAttributes, "Organizational Unit", "todo", name, "organizational-units"));
  }

  @PostMapping(path = "/admin/organizational-unit-delete")
  public String deleteOrganizationalUnit(
      @ModelAttribute(name = "ouDeleteRequest") OrganizationalUnitDeleteRequest ouDeleteRequest,
      ModelMap model,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {

    getLogger().debug("deleteOrganizationalUnit({})", ouDeleteRequest);

    if (!ouDeleteRequest.getName().equalsIgnoreCase(ouDeleteRequest.getVerificationName())) {
      bindingResult.rejectValue("verificationName", "todo", "The name doesn't match.");
      return "admin/organizational-unit-delete";
    }

    boolean result = organizationalUnitService.delete(ouDeleteRequest.getOuDn());

    model.clear();
    String name = ouDeleteRequest.getName();
    RedirectMessage rmsg;
    if (result) {
      rmsg = getRedirectMessage(RedirectMessageType.SUCCESS,
          String.format("Organizational unit '%s' was successfully deleted.", name),
          "todo", name);
    } else {
      rmsg = getRedirectMessage(RedirectMessageType.WARNING,
          String.format("Somehow the organizational unit '%s' was not deleted.", name),
          "todo", name);
    }
    redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, rmsg);

    Map<String, Object> parameters = getParamterMap();
    String redirect = getRedirectUri("organizational-units", PAGE_AND_OU_PARAMS, parameters);
    logRedirectTo("Organizational unit successfully added.", redirect);
    return redirect;
  }

}
