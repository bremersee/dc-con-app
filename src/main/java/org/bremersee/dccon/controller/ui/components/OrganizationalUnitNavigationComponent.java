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

package org.bremersee.dccon.controller.ui.components;

import jakarta.validation.constraints.NotNull;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.bremersee.dccon.controller.DomainControllerPropertiesProvider;
import org.bremersee.dccon.controller.ui.CurrentPageNameProvider;
import org.bremersee.dccon.controller.ui.model.OrganizationalUnitDropdown;
import org.bremersee.dccon.model.OrganizationalUnit;
import org.bremersee.dccon.service.OrganizationalUnitService;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;

/**
 * The interface OrganizationalUnitSelector.
 *
 * @author Christian Bremer
 */
@Validated
public interface OrganizationalUnitNavigationComponent extends DomainControllerPropertiesProvider,
    CurrentPageNameProvider {

  OrganizationalUnitService getOrganizationalUnitService();

  @NotNull
  Dn getDefaultOrganizationalUnit();

  SearchScope getDefaultSearchScope();

  default void addOrganizationalUnitDropdown(ModelMap model, OrganizationalUnitDropdown selector) {
    model.addAttribute(OU_DROPDOWN, selector);
  }

  default OrganizationalUnitDropdown getOrganizationalUnitDropdown(
      Dn ou,
      SearchScope scope) {

    Dn selectedOuDn = getProperties().getBaseDn(Optional.ofNullable(ou)
        .filter(dn -> getOrganizationalUnitService().organisationUnitExists(dn))
        .orElseGet(this::getDefaultOrganizationalUnit));
    List<OrganizationalUnit> orgUnits = getOrganizationalUnitService()
        .getOrganizationalUnitsWithBase()
        .sorted(getOrganizationalUnitComparator())
        .toList();
    OrganizationalUnitDropdown ouDropdown = new OrganizationalUnitDropdown();
    for (OrganizationalUnit orgUnit : orgUnits) {
      if (selectedOuDn.isSame(new Dn(orgUnit.getDistinguishedName()))) {
        ouDropdown.setSelectedOu(orgUnit);
      } else {
        ouDropdown.getSelectableOus().add(orgUnit);
      }
    }
    if (isBaseOu(ouDropdown.getSelectedOu())) {
      ouDropdown.setSelectedScope(SearchScope.SUBTREE);
      ouDropdown.setSelectedScopeDisplayValue(getDisplayValue(SearchScope.SUBTREE));
      ouDropdown.setSelectableScope(SearchScope.ONELEVEL);
      ouDropdown.setSelectableScopeDisplayValue(getDisplayValue(SearchScope.ONELEVEL));
      ouDropdown.setScopeSelectable(false);
    } else {
      SearchScope selectedScope = Optional.ofNullable(scope)
          .or(() -> Optional.ofNullable(getDefaultSearchScope()))
          .filter(s -> s == SearchScope.SUBTREE || s == SearchScope.ONELEVEL)
          .orElse(SearchScope.ONELEVEL);
      SearchScope selectableScope = selectedScope == SearchScope.SUBTREE
          ? SearchScope.ONELEVEL
          : SearchScope.SUBTREE;
      ouDropdown.setSelectedScope(selectedScope);
      ouDropdown.setSelectedScopeDisplayValue(getDisplayValue(selectedScope));
      ouDropdown.setSelectableScope(selectableScope);
      ouDropdown.setSelectableScopeDisplayValue(getDisplayValue(selectableScope));
      ouDropdown.setScopeSelectable(true);
    }
    return ouDropdown;
  }

  default boolean isBaseOu(OrganizationalUnit ou) {
    return getProperties().getBaseDn().isSame(new Dn(ou.getDistinguishedName()));
  }

  default Comparator<OrganizationalUnit> getOrganizationalUnitComparator() {
    return (o1, o2) -> {
      if (isBaseOu(o1)) {
        return -1;
      }
      if (isBaseOu(o2)) {
        return 1;
      }
      return o1.getNameTree().compareToIgnoreCase(o2.getNameTree());
    };
  }

  default String getDisplayValue(SearchScope scope) {
    return scope == SearchScope.ONELEVEL
        ? "One Level"
        : scope.name().substring(0, 1).toUpperCase() + scope.name().substring(1).toLowerCase();
  }

}
