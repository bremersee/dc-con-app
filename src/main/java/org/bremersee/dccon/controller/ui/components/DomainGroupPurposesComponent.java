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
import org.bremersee.dccon.model.DomainGroupType.Purpose;
import org.bremersee.dccon.model.SelectOption;

/**
 * The interface GroupTypesComponent.
 *
 * @author Christian Bremer
 */
public interface DomainGroupPurposesComponent extends MessageProvider {

  default List<SelectOption<String>> getDomainGroupPurposes() {
    return getDomainGroupPurposes(Purpose.SECURITY);
  }

  default List<SelectOption<String>> getDomainGroupPurposes(Purpose selectedPurpose) {
    return List.of(
        new SelectOption<>(
            Purpose.DISTRIBUTION.name(),
            getDisplayValue(Purpose.DISTRIBUTION),
            null,
            isSelected(Purpose.DISTRIBUTION, selectedPurpose),
            false,
            false),
        new SelectOption<>(
            Purpose.SECURITY.name(),
            getDisplayValue(Purpose.SECURITY),
            null,
            isSelected(Purpose.SECURITY, selectedPurpose),
            false,
            false)
    );
  }

  default String getDisplayValue(Purpose purpose) {
    return purpose.name().charAt(0) + purpose.name().substring(1).toLowerCase();
  }

  private boolean isSelected(Purpose groupPurpose, Purpose selectedPurpose) {
    Purpose selected = requireNonNullElse(selectedPurpose, Purpose.SECURITY);
    return selected.equals(groupPurpose);
  }

}
