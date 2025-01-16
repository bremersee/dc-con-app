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
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bremersee.dccon.model.OrganizationalUnit;
import org.ldaptive.dn.Dn;

/**
 * The type DomainGroupAddRequest.
 *
 * @author Christian Bremer
 */
@Data
@NoArgsConstructor
public class OrganizationalUnitEditRequest implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private String ou;

  private String parentOu;

  private String name;

  private String description;

  public OrganizationalUnitEditRequest(OrganizationalUnit ou) {
    this.ou = ou.getDistinguishedName();
    this.parentOu = ou.getDn().getParent().format();
    this.name = ou.getName();
    this.description = ou.getDescription();
  }

  public Dn getParentOuDn() {
    if (isEmpty(parentOu)) {
      return null;
    }
    return new Dn(parentOu);
  }

  public void update(OrganizationalUnit ou) {
    if (isEmpty(ou)) {
      return;
    }
    ou.setName(getName());
    ou.setDescription(getDescription());
  }

}
