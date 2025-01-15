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
import org.ldaptive.dn.Dn;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

/**
 * The type DomainGroupAddRequest.
 *
 * @author Christian Bremer
 */
@Data
@NoArgsConstructor
public class DomainGroupEditRequest implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  public static final DomainGroupEditMapper MAPPER = Mappers.getMapper(DomainGroupEditMapper.class);

  private String newOu;

  private String samAccountName;

  /**
   * The email address of the group.
   */
  private String email;

  /**
   * A description of the domain group.
   */
  private String description;

  /**
   * Group's Unix/RFC2307 GID number.
   */
  private Integer gidNumber;

  /**
   * Group's Unix/RFC2307 NIS domain.
   */
  private String nisDomain;

  public Dn getNewOuDn() {
    if (isEmpty(newOu)) {
      return null;
    }
    return new Dn(newOu);
  }

  @Mapper
  public interface DomainGroupEditMapper {

    @Mapping(source = "dn", target = "newOu")
    DomainGroupEditRequest map(DomainGroup domainGroup);

    default String maoToNewOu(Dn distinguishedName) {
      return Optional.ofNullable(distinguishedName)
          .map(Dn::getParent)
          .map(Dn::format)
          .orElse(null);
    }

    void update(@MappingTarget DomainGroup existingDomainGroup,
        DomainGroupEditRequest domainGroupEditRequest);
  }

}
