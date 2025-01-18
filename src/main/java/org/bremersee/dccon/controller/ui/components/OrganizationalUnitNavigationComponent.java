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

import java.util.List;
import java.util.Optional;
import org.bremersee.dccon.controller.DomainControllerPropertiesProvider;
import org.bremersee.dccon.controller.ui.CurrentPageNameProvider;
import org.bremersee.dccon.controller.ui.MessageProvider;
import org.bremersee.dccon.controller.ui.model.OrganizationalUnitDropdown;
import org.bremersee.dccon.model.OrganizationalUnit;
import org.bremersee.dccon.model.TreeSearchScope;
import org.bremersee.dccon.service.OrganizationalUnitService;
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
    CurrentPageNameProvider, MessageProvider {

  OrganizationalUnitService getOrganizationalUnitService();

  TreeSearchScope getDefaultSearchScope();

  default void addOrganizationalUnitDropdown(ModelMap model, OrganizationalUnitDropdown selector) {
    model.addAttribute(OU_DROPDOWN, selector);
  }

  default OrganizationalUnitDropdown getOrganizationalUnitDropdown(
      Dn ou,
      TreeSearchScope scope) {

    Dn selectedOuDn = getProperties().getBaseDn(Optional.ofNullable(ou)
        .filter(dn -> getOrganizationalUnitService().organisationUnitExists(dn))
        .orElse(null));
    List<OrganizationalUnit> orgUnits = getOrganizationalUnitService()
        .getOrganizationalUnitsWithSystemOusAndBase()
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
      ouDropdown.setSelectedScope(TreeSearchScope.SUBTREE);
      ouDropdown.setSelectedScopeDisplayValue(getDisplayValue(TreeSearchScope.SUBTREE));
      ouDropdown.setSelectableScope(TreeSearchScope.ONELEVEL);
      ouDropdown.setSelectableScopeDisplayValue(getDisplayValue(TreeSearchScope.ONELEVEL));
      ouDropdown.setScopeSelectable(false);
    } else {
      TreeSearchScope selectedScope = Optional.ofNullable(scope)
          .or(() -> Optional.ofNullable(getDefaultSearchScope()))
          .filter(s -> s == TreeSearchScope.SUBTREE || s == TreeSearchScope.ONELEVEL)
          .orElse(TreeSearchScope.ONELEVEL);
      TreeSearchScope selectableScope = selectedScope == TreeSearchScope.SUBTREE
          ? TreeSearchScope.ONELEVEL
          : TreeSearchScope.SUBTREE;
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

  default String getDisplayValue(TreeSearchScope scope) {
    return getMessage(scope.getDefaultDisplayName(), scope.getI18nCode());
  }

}
