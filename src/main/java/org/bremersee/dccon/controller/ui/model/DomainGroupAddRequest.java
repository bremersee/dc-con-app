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

package org.bremersee.dccon.controller.ui.model;

import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.model.DomainGroupType;
import org.bremersee.dccon.model.DomainGroupType.Purpose;
import org.bremersee.dccon.model.DomainGroupType.Scope;
import org.bremersee.dccon.model.DomainGroupTypeContainer;
import org.ldaptive.dn.Dn;

/**
 * The type DomainGroupAddRequest.
 *
 * @author Christian Bremer
 */
@Data
@NoArgsConstructor
public class DomainGroupAddRequest implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private DomainGroup group;

  private String ou;

  private String groupScope;

  private String groupPurpose;

  public DomainGroupAddRequest(DomainGroup group, String ou) {
    this.group = group;
    this.ou = ou;
    this.groupScope = Optional.ofNullable(group)
        .map(DomainGroup::getGroupType)
        .map(DomainGroupTypeContainer::getGroupType)
        .map(DomainGroupType::getScope)
        .map(Enum::name)
        .orElse(Scope.GLOBAL.name());
    this.groupPurpose = Optional.ofNullable(group)
        .map(DomainGroup::getGroupType)
        .map(DomainGroupTypeContainer::getGroupType)
        .map(DomainGroupType::getPurpose)
        .map(Enum::name)
        .orElse(Purpose.SECURITY.name());
  }

  public Dn getOuDn() {
    if (isEmpty(ou)) {
      return null;
    }
    return new Dn(ou);
  }

  @Override
  public String toString() {
    return "DomainUserAddRequest {"
        + "group=" + Optional.ofNullable(group).map(DomainGroup::getSamAccountName).orElse(null)
        + ", ou=" + Optional.ofNullable(getOuDn()).map(Dn::format).orElse(null)
        + ", scope=" + getGroupScope()
        + ", purpose=" + getGroupPurpose()
        + '}';
  }
}
