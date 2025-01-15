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

import static java.util.Objects.isNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.Hidden;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ldaptive.dn.Dn;

/**
 * Common attributes.
 *
 * @author Christian Bremer
 */
@Data
@NoArgsConstructor
public abstract class CommonAttributes implements DistinguishedNameProvider {

  /**
   * The distinguished name in the active directory.
   */
  @Hidden
  @JsonIgnore
  Dn dn;

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

    setDistinguishedName(distinguishedName);
    setCreated(created);
    setModified(modified);
  }

  public CommonAttributes(
      Dn dn,
      OffsetDateTime created,
      OffsetDateTime modified) {

    setDn(dn);
    setCreated(created);
    setModified(modified);
  }

  @Hidden
  @JsonIgnore
  public Dn getDn() {
    return dn;
  }

  @Hidden
  @JsonIgnore
  public void setDn(Dn dn) {
    this.dn = dn;
  }

  public String getDistinguishedName() {
    return isNull(dn) ? null : dn.format();
  }

  public void setDistinguishedName(String distinguishedName) {
    if (isNull(distinguishedName) || distinguishedName.isEmpty()) {
      this.dn = null;
    } else {
      this.dn = new Dn(distinguishedName);
    }
  }

  @Hidden
  @JsonIgnore
  public String getParentDistinguishedName() {
    return Optional.ofNullable(getDn())
        .map(Dn::getParent)
        .map(Dn::format)
        .orElse(null);
  }

  @Hidden
  @JsonIgnore
  public String getNameTree() { // ou is reverse
    return Stream.ofNullable(getDn())
        .map(Dn::getRDns)
        .flatMap(Collection::stream)
        .filter(rdn -> !rdn.getNameValue().hasName("dc"))
        .map(rdn -> rdn.getNameValue().getStringValue())
        .collect(Collectors.joining(" → "));
  }

}

