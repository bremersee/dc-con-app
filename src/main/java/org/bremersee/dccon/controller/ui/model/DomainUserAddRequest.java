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
import lombok.Data;
import org.bremersee.dccon.model.DomainUser;
import org.ldaptive.dn.Dn;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * The type DomainUserAddRequest.
 *
 * @author Christian Bremer
 */
@Data
public class DomainUserAddRequest implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private String newOu;

  private String samAccountName;

  private boolean useUsernameAsCn = true;

  private boolean enabled = true;

  private boolean passwordExpirationEnabled = false;

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

  private boolean sendEmail;

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

  /**
   * User's password.
   */
  private String password;

  private boolean generateRandomPassword;

  public Dn getNewOuDn() {
    if (isEmpty(newOu)) {
      return null;
    }
    return new Dn(newOu);
  }

  @Mapper
  public interface DomainMapper {

    DomainMapper INSTANCE = Mappers.getMapper(DomainMapper.class);

    @Mapping(source = "enabled", target = "accountControl.enabled")
    @Mapping(
        source = "passwordExpirationEnabled",
        target = "accountControl.passwordExpirationEnabled")
    DomainUser mapToDomainUser(DomainUserAddRequest request);

  }
}
