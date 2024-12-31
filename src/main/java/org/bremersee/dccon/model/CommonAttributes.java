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

package org.bremersee.dccon.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Common attributes.
 *
 * @author Christian Bremer
 */
@Data
@NoArgsConstructor
public abstract class CommonAttributes implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * The distinguished name in the active directory.
   */
  String distinguishedName;

  /**
   * The creation date.
   */
  OffsetDateTime created;

  /**
   * The last modification date.
   */
  OffsetDateTime modified;

  /**
   * Instantiates a new common attributes.
   *
   * @param distinguishedName the distinguished name
   * @param created the created
   * @param modified the modified
   */
  public CommonAttributes(
      String distinguishedName,
      OffsetDateTime created,
      OffsetDateTime modified) {

    this.distinguishedName = distinguishedName;
    this.created = created;
    this.modified = modified;
  }

}

