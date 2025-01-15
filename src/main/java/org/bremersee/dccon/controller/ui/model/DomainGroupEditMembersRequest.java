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

import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bremersee.dccon.model.DomainGroup;

/**
 * The type DomainGroupAddRequest.
 *
 * @author Christian Bremer
 */
@Setter
@NoArgsConstructor
public class DomainGroupEditMembersRequest implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private List<String> membersBase64;

  public DomainGroupEditMembersRequest(DomainGroup domainGroup) {
    this.membersBase64 = Stream.ofNullable(domainGroup)
        .map(DomainGroup::getMembers)
        .flatMap(Collection::stream)
        .map(dn -> Base64.getEncoder().encodeToString(dn.getBytes(StandardCharsets.UTF_8)))
        .toList();
  }

  public List<String> getMembersBase64() {
    if (isNull(membersBase64)) {
      membersBase64 = new ArrayList<>();
    }
    return membersBase64;
  }

  public List<String> getMembers() {
    return getMembersBase64().stream()
        .map(base64 -> new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8))
        .toList();
  }

  @Override
  public String toString() {
    return "DomainGroupEditMembersRequest{"
        + "members=" + getMembers()
        + '}';
  }
}
