package org.bremersee.dccon.controller.ui.components;

import java.util.Optional;
import org.bremersee.dccon.controller.ui.ControllerConstants;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

public interface OrganizationalUnitComponent extends ControllerConstants {

  @ModelAttribute(OU)
  default String addOrganisationalUnit(@RequestParam(name = OU, required = false) Dn ou) {
    return Optional.ofNullable(ou).map(Dn::format).orElse("");
  }

  @ModelAttribute(SCOPE)
  default String addSearchScope(@RequestParam(name = SCOPE, required = false) SearchScope scope) {
    return Optional.ofNullable(scope).map(Enum::name).orElse("");
  }

}
