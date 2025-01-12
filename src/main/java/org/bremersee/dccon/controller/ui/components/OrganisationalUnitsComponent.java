package org.bremersee.dccon.controller.ui.components;

import java.util.Comparator;
import java.util.List;
import org.bremersee.dccon.model.OrganizationalUnit;
import org.bremersee.dccon.service.OrganizationalUnitService;
import org.springframework.web.bind.annotation.ModelAttribute;

public interface OrganisationalUnitsComponent {

  OrganizationalUnitService getOrganizationalUnitService();

  @ModelAttribute("ous")
  default List<OrganizationalUnit> addOrganisationalUnits() {
    return getOrganizationalUnitService().getOrganizationalUnits()
        .toList();
  }

}
