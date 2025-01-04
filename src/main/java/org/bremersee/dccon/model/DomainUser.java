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
import static java.util.Objects.nonNull;

import java.io.Serial;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A domain (Active Directory) user may represent physical entities, such as people or may be used
 * as service accounts for applications. User accounts are also referred to as security principals
 * and are assigned a security identifier (SID).
 *
 * <p>A user account enables a user to logon to a computer and domain with an identity that can be
 * authenticated. To maximize security, each user should have their own unique user account and
 * password. A user's access to domain resources is based on permissions assigned to the user
 * account.
 *
 * <p>This main representation has a membership attribute with the distinguished names of the
 * groups.
 *
 * @author Christian Bremer
 */
@Getter
@Setter
@ToString(callSuper = true, exclude = {"password"})
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class DomainUser extends CommonAttributes
    implements SamAccount, NisDomainMember {

  @Serial
  private static final long serialVersionUID = 2L;

  /**
   * User's account control.
   */
  DomainUserAccountControl accountControl = DomainUserAccountControl.builder().build();

  /**
   * User's last logon time.
   */
  OffsetDateTime accountExpires;

  /**
   * User's company.
   */
  String company;

  /**
   * User's department.
   */
  String department;

  /**
   * A description of the user.
   */
  String description;

  /**
   * User's display name.
   */
  String displayName;

  /**
   * User's email address.
   */
  String email;

  /**
   * User's first name.
   */
  String firstName;

  /**
   * User's Unix/RFC2307 GECOS field.
   */
  String gecos;

  /**
   * User's Unix/RFC2307 primary GID number.
   */
  Integer gidNumber;

  /**
   * User's home directory path.
   */
  String homeDirectory;

  /**
   * User's home drive letter.
   */
  String homeDrive;

  /**
   * User's initials.
   */
  String initials;

  /**
   * User's last logon time.
   */
  OffsetDateTime lastLogon;

  /**
   * User's last name.
   */
  String lastName;

  /**
   * User's Unix/RFC2307 login shell.
   */
  String loginShell;

  /**
   * User's logon count.
   */
  Integer logonCount;

  /**
   * User's group membership.
   */
  List<String> membership;

  /**
   * User's mobile phone number.
   */
  String mobile;

  /**
   * User's Unix/RFC2307 NIS domain.
   */
  String nisDomain;

  /**
   * User's primary group ID.
   */
  Integer primaryGroupId;

  /**
   * User's password.
   */
  String password;

  /**
   * Timestamp of the last password change.
   */
  OffsetDateTime passwordLastSet;

  /**
   * User's office location.
   */
  String physicalDeliveryOfficeName;

  /**
   * User's preferred language. ISO 639-1 language codes. The combinations like de-DE and en-US with
   * ISO-639 and ISO-3166 also work.
   */
  String preferredLanguage;

  /**
   * User's profile path.
   */
  String profilePath;

  /**
   * User's username.
   */
  String samAccountName;

  /**
   * User's logon script path.
   */
  String scriptPath;

  /**
   * User's windows/samba SID.
   */
  Sid sid;

  /**
   * User's telephone number.
   */
  String telephoneNumber;

  /**
   * User's job title.
   */
  String title;

  /**
   * User's Unix/RFC2307 username.
   */
  String uid;

  /**
   * User's Unix/RFC2307 numeric UID.
   */
  Integer uidNumber;

  /**
   * User's Unix/RFC2307 home directory.
   */
  String unixHomeDirectory;

  /**
   * User's principal name.
   */
  String userPrincipalName;

  public Locale getLocale(Locale defaultLocale) {
    if (isNull(getPreferredLanguage()) || getPreferredLanguage().isEmpty()) {
      return defaultLocale;
    }
    Locale locale = Locale.forLanguageTag(getPreferredLanguage());
    if (isNull(locale) || locale.getLanguage().isEmpty()) {
      return defaultLocale;
    }
    return locale;
  }

  /**
   * User's group membership.
   *
   * @return the group membership
   */
  public List<String> getMembership() {
    if (membership == null) {
      membership = new ArrayList<>();
    }
    return membership;
  }

  /**
   * Sets user's account control.
   *
   * @param accountControl user's account control
   */
  public void setAccountControl(DomainUserAccountControl accountControl) {
    if (nonNull(accountControl)) {
      this.accountControl = accountControl;
    }
  }

}
