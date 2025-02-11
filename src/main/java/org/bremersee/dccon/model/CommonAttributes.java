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
import static java.util.Objects.nonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.Hidden;
import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.ldaptive.dn.Dn;

/**
 * Common attributes.
 *
 * @author Christian Bremer
 */
@Getter
@Setter
@ToString(exclude = {"dn"})
@EqualsAndHashCode(exclude = {"dn"})
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public abstract class CommonAttributes implements Serializable, DistinguishedNameProvider {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * The distinguished name in the active directory.
   */
  @Hidden
  @JsonIgnore
  transient Dn dn;

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
    if (isNull(dn) && nonNull(distinguishedName)) {
      dn = new Dn(distinguishedName);
    }
    return dn;
  }

  @Hidden
  @JsonIgnore
  public void setDn(Dn dn) {
    if (isNull(dn) || dn.isEmpty()) {
      this.dn = null;
      this.distinguishedName = null;
    } else {
      this.dn = dn;
      this.distinguishedName = dn.format();
    }
  }

  public String getDistinguishedName() {
    return isNull(getDn()) ? null : getDn().format();
  }

  public void setDistinguishedName(String distinguishedName) {
    if (isNull(distinguishedName) || distinguishedName.isEmpty()) {
      this.dn = null;
      this.distinguishedName = null;
    } else {
      setDn(new Dn(distinguishedName));
    }
  }

  @Hidden
  @JsonIgnore
  public String getDistinguishedNameUnformatted() {
    return isNull(getDn()) ? null : getDn().format(rdn -> rdn);
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

