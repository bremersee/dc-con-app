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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.bremersee.dccon.controller.DomainControllerPropertiesProvider;
import org.bremersee.dccon.controller.ui.CurrentPageNameProvider;
import org.bremersee.dccon.controller.ui.model.OrganizationalUnitSelector;
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
public interface OrganizationalUnitSelectorComponent extends DomainControllerPropertiesProvider,
    CurrentPageNameProvider {

  OrganizationalUnitService getOrganizationalUnitService();

  String getDefaultOrganizationalUnit();

  SearchScope getDefaultSearchScope();

  default void addOrganizationalUnitSelector(ModelMap model, OrganizationalUnitSelector selector) {
    model.addAttribute(OU_SELECTOR, selector);
  }

  default OrganizationalUnitSelector getOrganizationalUnitSelector(
      Dn ou,
      SearchScope scope) {

    OrganizationalUnitSelector ouSelector = new OrganizationalUnitSelector();
    OrganizationalUnit selectedOu = Optional.ofNullable(ou)
        .or(() -> Optional.ofNullable(getDefaultOrganizationalUnit()).map(Dn::new))
        .flatMap(ouDn -> getOrganizationalUnitService().getOrganizationalUnit(ouDn))
        .orElseGet(() -> getOrganizationalUnitService().getBase());
    ouSelector.setSelectedOu(selectedOu);
    List<OrganizationalUnit> selectableOus = getOrganizationalUnitService()
        .getOrganizationalUnitsWithBaseButWithoutSelected(new Dn(selectedOu.getDistinguishedName()))
        .sorted(getOrganizationalUnitComparator())
        .toList();
    ouSelector.setSelectableOus(selectableOus);
    if (isBaseOu(selectedOu)) {
      ouSelector.setSelectedScope(SearchScope.SUBTREE);
      ouSelector.setSelectedScopeDisplayValue(getDisplayValue(SearchScope.SUBTREE));
      ouSelector.setSelectableScope(SearchScope.ONELEVEL);
      ouSelector.setSelectableScopeDisplayValue(getDisplayValue(SearchScope.ONELEVEL));
      ouSelector.setScopeSelectable(false);
    } else {
      SearchScope selectedScope = Optional.ofNullable(scope)
          .or(() -> Optional.ofNullable(getDefaultSearchScope()))
          .filter(s -> s == SearchScope.SUBTREE || s == SearchScope.ONELEVEL)
          .orElse(SearchScope.ONELEVEL);
      SearchScope selectableScope = selectedScope == SearchScope.SUBTREE
          ? SearchScope.ONELEVEL
          : SearchScope.SUBTREE;
      ouSelector.setSelectedScope(selectedScope);
      ouSelector.setSelectedScopeDisplayValue(getDisplayValue(selectedScope));
      ouSelector.setSelectableScope(selectableScope);
      ouSelector.setSelectableScopeDisplayValue(getDisplayValue(selectableScope));
      ouSelector.setScopeSelectable(true);
    }
    return ouSelector;
  }

  default boolean isBaseOu(OrganizationalUnit ou) {
    return new Dn(getProperties().getBaseDn()).isSame(new Dn(ou.getDistinguishedName()));
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

  /*
  @ModelAttribute(OU)
  default String addSelectedOrganizationalUnitRdn(
      @RequestParam(name = OU, required = false) Dn ou) {
    return Optional.ofNullable(ou)
        .map(Dn::format)
        .orElse(requireNonNullElse(getDefaultOrganizationalUnit(), ""));
  }

  @ModelAttribute(SCOPE)
  default String addSelectedSearchScope(
      @RequestParam(name = OU, required = false) Dn ou,
      @RequestParam(name = SCOPE, required = false) SearchScope scope) {
    OrganizationalUnit selectedUnit = addSelectedOrganizationalUnit(ou);
    if (new Dn(selectedUnit.getDistinguishedName()).getRDn().getNameValue().hasName("dc")) {
      return getDisplayValue(SearchScope.SUBTREE);
    }
    return getDisplayValue(Optional.ofNullable(scope)
        .filter(s -> s == SearchScope.SUBTREE || s == SearchScope.ONELEVEL)
        .orElse(SearchScope.ONELEVEL));
  }
  */

}
