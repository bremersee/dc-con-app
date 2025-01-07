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

package org.bremersee.dccon.controller.ui.components;

import static java.util.Objects.requireNonNullElse;

import java.util.List;
import org.bremersee.dccon.controller.ui.MessageProvider;
import org.bremersee.dccon.model.DomainGroupType.Scope;
import org.bremersee.dccon.model.SelectOption;

/**
 * The interface GroupScopesComponent.
 *
 * @author Christian Bremer
 */
public interface DomainGroupScopesComponent extends MessageProvider {

  default List<SelectOption<String>> getDomainGroupScopes() {
    return getDomainGroupScopes(Scope.GLOBAL);
  }

  default List<SelectOption<String>> getDomainGroupScopes(Scope selectedGroupScope) {
    return List.of(
        new SelectOption<>(
            Scope.DOMAIN_LOCAL.name(),
            getDisplayValue(Scope.DOMAIN_LOCAL),
            null,
            isSelected(Scope.DOMAIN_LOCAL, selectedGroupScope),
            false,
            false),
        new SelectOption<>(
            Scope.GLOBAL.name(),
            getDisplayValue(Scope.GLOBAL),
            null,
            isSelected(Scope.GLOBAL, selectedGroupScope),
            false,
            false),
        new SelectOption<>(
            Scope.UNIVERSAL.name(),
            getDisplayValue(Scope.UNIVERSAL),
            null,
            isSelected(Scope.UNIVERSAL, selectedGroupScope),
            false,
            false)
    );
  }

  default String getDisplayValue(Scope scope) {
    return scope.toString();
  }

  private boolean isSelected(Scope groupScope, Scope selectedGroupScope) {
    Scope selected = requireNonNullElse(selectedGroupScope, Scope.GLOBAL);
    return selected.equals(groupScope);
  }

}
