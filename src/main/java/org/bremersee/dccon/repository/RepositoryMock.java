/*
 * Copyright 2019-2020 the original author or authors.
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

package org.bremersee.dccon.repository;

import static org.springframework.util.ObjectUtils.isEmpty;

import java.time.OffsetDateTime;
import org.bremersee.dccon.model.CommonAttributes;
import org.ldaptive.dn.Dn;
import org.ldaptive.dn.NameValue;
import org.ldaptive.dn.RDn;

/**
 * The mock repository interface.
 *
 * @author Christian Bremer
 */
public interface RepositoryMock extends RepositoryMockConstants {

  /**
   * Reset data.
   */
  void resetData();

  /*
  Dn getDefaultOu();

  Dn getBaseDn(Dn ou);

  default void updateCommonAttributes(CommonAttributes entity, Dn ou, String rdnName,
      String rdnValue) {
    Dn baseDn;
    if (isEmpty(ou) || ou.isEmpty()) {
      baseDn = getBaseDn(getDefaultOu());
    } else {
      baseDn = getBaseDn(ou);
    }
    Dn dn = new Dn(new RDn(new NameValue(rdnName, rdnValue)));
    dn.add(baseDn);
    entity.setDistinguishedName(dn.format());
    if (isEmpty(entity.getCreated())) {
      entity.setCreated(OffsetDateTime.now());
    }
    entity.setModified(OffsetDateTime.now());
  }
  */
}
