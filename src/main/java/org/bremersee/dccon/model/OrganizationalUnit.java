/*
 * Copyright 2024 the original author or authors.
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

import java.io.Serial;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.lang.NonNull;

/**
 * The type OrganisationUnit.
 *
 * @author Christian Bremer
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class OrganizationalUnit extends CommonAttributes
    implements Comparable<OrganizationalUnit> {

  @Serial
  private static final long serialVersionUID = 1L;

  private String description;

  private String name;

  private Boolean systemOu;

  @Builder(toBuilder = true)
  public OrganizationalUnit(String distinguishedName, OffsetDateTime created,
      OffsetDateTime modified, String description, String name,
      Boolean systemOu) {
    super(distinguishedName, created, modified);
    this.description = description;
    this.name = name;
    this.systemOu = systemOu;
  }

  public Boolean getSystemOu() {
    return Boolean.TRUE.equals(systemOu);
  }

  public String getNameTree() {
    String dn = getDistinguishedName();
    if (isNull(dn)) {
      return null;
    }
    if (dn.toLowerCase().startsWith("dc=")) {
      return getName();
    }
    int index = dn.toLowerCase().indexOf(",dc=");
    if (index != -1) {
      dn = dn.substring(0, index);
    }
    List<String> names = new ArrayList<>();
    StringTokenizer st = new StringTokenizer(dn, ",");
    while (st.hasMoreTokens()) {
      String rdn = st.nextToken();
      index = rdn.indexOf('=');
      if (index != -1) {
        names.add(rdn.substring(index + 1));
      } else {
        names.add(rdn);
      }
    }
    Collections.reverse(names);
    return String.join(" → ", names);
  }

  @Override
  public int compareTo(@NonNull OrganizationalUnit o) {
    String s0 = Objects.requireNonNullElse(getDistinguishedName(), "");
    String s1 = Objects.requireNonNullElse(o.getDistinguishedName(), "");
    return s0.compareToIgnoreCase(s1);
  }
}
