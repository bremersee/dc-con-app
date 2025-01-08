package org.bremersee.dccon.controller.ui.components;

import java.util.Optional;
import org.bremersee.dccon.controller.ui.ControllerConstants;
import org.bremersee.dccon.controller.ui.LoggerProvider;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

public interface OrganizationalUnitComponent extends ControllerConstants, LoggerProvider {

  @ModelAttribute(OU)
  default String addOrganisationalUnit(@RequestParam(name = OU, required = false) Dn ou) {
    String currentOu = Optional.ofNullable(ou).map(Dn::format).orElse("");
    getLogger().debug("Adding 'ou={}' to model.", currentOu);
    return currentOu;
  }

  @ModelAttribute(SCOPE)
  default String addSearchScope(@RequestParam(name = SCOPE, required = false) SearchScope scope) {
    String searchScope = Optional.ofNullable(scope).map(Enum::name).orElse("");
    getLogger().debug("Adding 'scope={}' to model.", searchScope);
    return searchScope;
  }

}
