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

import java.util.List;
import java.util.stream.Stream;
import org.bremersee.dccon.controller.ui.MessageProvider;
import org.bremersee.dccon.model.DomainGroupType.Purpose;
import org.bremersee.dccon.model.DomainGroupType.Scope;
import org.bremersee.dccon.model.SelectOption;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * The domain group types component.
 *
 * @author Christian Bremer
 */
public interface DomainGroupTypesComponent extends MessageProvider {

  @ModelAttribute("groupScopes")
  default List<SelectOption<String>> addDomainGroupScopes() {
    return Stream.of(
            new SelectOption<>(
                Scope.DOMAIN_LOCAL.name(),
                getDisplayValue(Scope.DOMAIN_LOCAL),
                getDisplayValue(Scope.DOMAIN_LOCAL),
                false,
                false,
                false),
            new SelectOption<>(
                Scope.GLOBAL.name(),
                getDisplayValue(Scope.GLOBAL),
                getDisplayValue(Scope.GLOBAL),
                false,
                false,
                false),
            new SelectOption<>(
                Scope.UNIVERSAL.name(),
                getDisplayValue(Scope.UNIVERSAL),
                getDisplayValue(Scope.UNIVERSAL),
                false,
                false,
                false))
        .sorted()
        .toList();
  }

  default String getDisplayValue(Scope scope) {
    return scope.toString();
  }

  @ModelAttribute("groupPurposes")
  default List<SelectOption<String>> addDomainGroupPurposes() {
    return Stream.of(
            new SelectOption<>(
                Purpose.DISTRIBUTION.name(),
                getDisplayValue(Purpose.DISTRIBUTION),
                getDisplayValue(Purpose.DISTRIBUTION),
                false,
                false,
                false),
            new SelectOption<>(
                Purpose.SECURITY.name(),
                getDisplayValue(Purpose.SECURITY),
                getDisplayValue(Purpose.SECURITY),
                false,
                false,
                false))
        .sorted()
        .toList();
  }

  default String getDisplayValue(Purpose purpose) {
    return purpose.toString();
  }

}
