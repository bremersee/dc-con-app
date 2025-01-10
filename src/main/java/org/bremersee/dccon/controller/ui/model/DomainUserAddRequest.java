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

import static java.util.Objects.isNull;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;
import lombok.Data;
import org.bremersee.dccon.model.DomainUser;
import org.ldaptive.dn.Dn;

/**
 * The type DomainUserAddRequest.
 *
 * @author Christian Bremer
 */
@Data
public class DomainUserAddRequest implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private DomainUser user;

  private String newOu;

  private boolean useUsernameAsCn = true;

  private boolean sendEmail;

  private boolean generateRandomPassword;

  public Dn getNewOuDn() {
    if (isEmpty(newOu)) {
      return null;
    }
    return new Dn(newOu);
  }

  @Override
  public String toString() {
    return "DomainUserAddRequest {"
        + "user=" + Optional.ofNullable(user).map(DomainUser::getSamAccountName).orElse(null)
        + ", newOu=" + Optional.ofNullable(getNewOuDn()).map(Dn::format).orElse(null)
        + ", useUsernameAsCn=" + useUsernameAsCn
        + ", sendEmail=" + sendEmail
        + ", generateRandomPassword=" + generateRandomPassword
        + '}';
  }
}
