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
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;
import org.springframework.web.multipart.MultipartFile;

/**
 * The type DomainUserEditRequest.
 *
 * @author Christian Bremer
 */
@Data
@NoArgsConstructor
public class DomainUserEditRequest {

  public static final DomainUserEditMapper MAPPER = Mappers.getMapper(DomainUserEditMapper.class);

  private MultipartFile avatar;

  private boolean removeAvatar;

  private String newOu;

  private String samAccountName;

  private boolean renameNamesAutomatically = true;

  private boolean enabled = true;

  private boolean passwordExpirationEnabled = false;

  private String userPrincipalName;

  private Integer primaryGroupId;

  /**
   * User's first name.
   */
  private String firstName;

  /**
   * User's last name.
   */
  private String lastName;

  /**
   * User's display name.
   */
  private String displayName;

  /**
   * User's initials.
   */
  private String initials;

  /**
   * User's preferred language. ISO 639-1 language codes. The combinations like de-DE and en-US with
   * ISO-639 and ISO-3166 also work.
   */
  private String preferredLanguage;

  /**
   * User's email address.
   */
  private String email;

  /**
   * User's mobile phone number.
   */
  private String mobile;

  /**
   * User's telephone number.
   */
  private String telephoneNumber;

  /**
   * A description of the user.
   */
  private String description;

  /**
   * User's home directory path.
   */
  private String homeDirectory;

  /**
   * User's home drive letter.
   */
  private String homeDrive;

  /**
   * User's profile path.
   */
  private String profilePath;

  /**
   * User's logon script path.
   */
  private String scriptPath;

  /**
   * User's company.
   */
  private String company;

  /**
   * User's job title.
   */
  private String title;

  /**
   * User's department.
   */
  private String department;

  /**
   * User's office location.
   */
  private String physicalDeliveryOfficeName;

  /**
   * User's Unix/RFC2307 username.
   */
  private String uid;

  /**
   * User's Unix/RFC2307 numeric UID.
   */
  private Integer uidNumber;

  /**
   * User's Unix/RFC2307 primary GID number.
   */
  private Integer gidNumber;

  /**
   * User's Unix/RFC2307 login shell.
   */
  private String loginShell;

  /**
   * User's Unix/RFC2307 home directory.
   */
  private String unixHomeDirectory;

  /**
   * User's Unix/RFC2307 GECOS field.
   */
  private String gecos;

  /**
   * User's Unix/RFC2307 NIS domain.
   */
  private String nisDomain;

  public Dn getNewOuDn() {
    if (isEmpty(newOu)) {
      return null;
    }
    return new Dn(newOu);
  }

  @Mapper
  public interface DomainUserEditMapper {

    @Mapping(source = "dn", target = "newOu")
    @Mapping(source = "accountControl.enabled", target = "enabled")
    @Mapping(
        source = "accountControl.passwordExpirationEnabled",
        target = "passwordExpirationEnabled")
    DomainUserEditRequest map(DomainUser domainUser);

    default String mapToNewOu(Dn distinguishedName) {
      return Optional.ofNullable(distinguishedName)
          .map(Dn::getParent)
          .map(Dn::format)
          .orElse(null);
    }

    @Mapping(source = "enabled", target = "accountControl.enabled")
    @Mapping(
        source = "passwordExpirationEnabled",
        target = "accountControl.passwordExpirationEnabled")
    void update(@MappingTarget DomainUser existingDomainUser,
        DomainUserEditRequest domainUserEditRequest);
  }

}
