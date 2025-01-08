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

import java.util.Optional;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bremersee.dccon.model.DomainUser;
import org.ldaptive.dn.Dn;
import org.springframework.web.multipart.MultipartFile;

/**
 * The type DomainUserEditRequest.
 *
 * @author Christian Bremer
 */
@Data
@NoArgsConstructor
public class DomainUserEditRequest {

  private String oldSamAccountName;

  private String oldFirstName;

  private String oldLastName;

  private String oldName;

  private boolean renameNamesAutomatically = true;

  private DomainUser user;

  private String newOu;

  private MultipartFile avatar;

  private boolean removeAvatar;

  public DomainUserEditRequest(DomainUser user, Dn newOu) {
    this.oldSamAccountName = user.getSamAccountName();
    this.oldFirstName = user.getFirstName();
    this.oldLastName = user.getLastName();
    this.oldName = user.getName();
    this.user = user;
    this.newOu = newOu.format();
  }

  public Dn getNewOuDn() {
    if (isEmpty(newOu)) {
      return null;
    }
    return new Dn(newOu);
  }

  @Override
  public String toString() {
    return "DomainUserEditRequest {"
        + "oldSamAccountName=" + oldSamAccountName
        + ", oldFirstName=" + oldFirstName
        + ", oldLastName=" + oldLastName
        + ", user=" + Optional.ofNullable(user).map(DomainUser::getSamAccountName).orElse(null)
        + ", newOu=" + Optional.ofNullable(getNewOuDn()).map(Dn::format).orElse(null)
        + '}';
  }

}
