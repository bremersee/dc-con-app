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

/**
 * @author Christian Bremer
 */
public interface DomainUserRepositoryConstants extends RepositoryConstants {

  //String OBJECT_CLASS = CommonLdapConstants.OBJECT_CLASS;

  //String CN = CommonLdapConstants.CN;

  String LDAP_USER_ACCOUNT_EXPIRES = "accountExpires";

  //String CODE_PAGE = "codePage"; // language, no mapping

  String LDAP_USER_COMPANY = "company";

  //String COUNTRY_CODE = "countryCode"; // integer, no mapping

  String LDAP_USER_DEPARTMENT = "department";

  //String DESCRIPTION = CommonLdapConstants.DESCRIPTION;

  /**
   * The constant DISPLAY_NAME. Attribute of the display name (first name [, initials] and last
   * name).
   */
  String LDAP_USER_DISPLAY_NAME = "displayName";

  /**
   * The constant GECOS. Attribute of the display name (first name and last name, rfc2307).
   */
  String LDAP_USER_GECOS = "gecos";

  //String GID_NUMBER = CommonLdapConstants.GID_NUMBER;

  String LDAP_USER_GIVEN_NAME = "givenName";

  String LDAP_USER_HOME_DIRECTORY = "homeDirectory";

  String LDAP_USER_HOME_DRIVE = "homeDrive";

  String LDAP_USER_INITIALS = "initials";

  String LDAP_USER_JPEG_PHOTO = "jpegPhoto";

  String LDAP_USER_LAST_LOGON = "lastLogon";

  String LDAP_USER_LOGIN_SHELL = "loginShell";

  String LDAP_USER_LOGON_COUNT = "logonCount";

  //String MAIL = CommonLdapConstants.MAIL;

  //String MEMBER_OF = CommonLdapConstants.MEMBER_OF;

  String LDAP_USER_MOBILE = "mobile";

  //String NAME = CommonLdapConstants.NAME;

  //String NIS_DOMAIN = CommonLdapConstants.NIS_DOMAIN;

  //String NIS_NAME = CommonLdapConstants.NIS_NAME;

  //String OBJECT_SID = CommonLdapConstants.OBJECT_SID;

  String LDAP_USER_OFFICE_NAME = "physicalDeliveryOfficeName";

  String LDAP_USER_PREFERRED_LANGUAGE = "preferredLanguage";

  int LDAP_USER_PRIMARY_GROUP_ID_DOMAIN_USERS_VALUE = 513;

  String LDAP_USER_PROFILE_PATH = "profilePath";

  String LDAP_USER_PWD_LAST_SET = "pwdLastSet";

  //String SAM_ACCOUNT_NAME = CommonLdapConstants.SAM_ACCOUNT_NAME;

  String LDAP_USER_SCRIPT_PATH = "scriptPath";

  String LDAP_USER_SN = "sn";

  String LDAP_USER_TELEPHONE_NUMBER = "telephoneNumber";

  String LDAP_USER_TITLE = "title";

  /**
   * The constant UID. Attribute of the username.
   */
  String LDAP_USER_UID = "uid"; // TODO msSFU30Name

  String LDAP_USER_UID_NUMBER = "uidNumber";

  String LDAP_USER_UNICODE_PWD = "unicodePwd";

  String LDAP_USER_UNIX_HOME_DIRECTORY = "unixHomeDirectory";

  String LDAP_USER_USER_ACCOUNT_CONTROL = "userAccountControl";

  String LDAP_USER_USER_PRINCIPAL_NAME = "userPrincipalName";

  String[] LDAP_USER_BINARY_ATTRIBUTES = {
      LDAP_USER_JPEG_PHOTO,
      LDAP_OBJECT_SID
  };

  String[] LDAP_USER_MAPPED_BINARY_ATTRIBUTES = {
      LDAP_OBJECT_SID
  };

  String[] LDAP_USER_MAPPED_ATTRIBUTES = {
      LDAP_WHEN_CREATED,
      LDAP_WHEN_CHANGED,
      LDAP_USER_ACCOUNT_EXPIRES,
      LDAP_USER_COMPANY,
      LDAP_USER_DEPARTMENT,
      LDAP_DESCRIPTION,
      LDAP_USER_DISPLAY_NAME,
      LDAP_USER_GECOS,
      LDAP_USER_GIVEN_NAME,
      LDAP_GID_NUMBER,
      LDAP_USER_HOME_DIRECTORY,
      LDAP_USER_HOME_DRIVE,
      LDAP_USER_INITIALS,
      LDAP_USER_LAST_LOGON,
      LDAP_USER_LOGIN_SHELL,
      LDAP_USER_LOGON_COUNT,
      LDAP_MAIL,
      LDAP_MEMBER_OF_GROUP,
      LDAP_USER_MOBILE,
      LDAP_NIS_DOMAIN,
      LDAP_NIS_NAME,
      LDAP_OBJECT_SID,
      LDAP_USER_OFFICE_NAME,
      LDAP_USER_PREFERRED_LANGUAGE,
      LDAP_PRIMARY_GROUP_ID,
      LDAP_USER_PROFILE_PATH,
      LDAP_USER_PWD_LAST_SET,
      LDAP_SAM_ACCOUNT_NAME,
      LDAP_USER_SCRIPT_PATH,
      LDAP_USER_SN,
      LDAP_USER_TELEPHONE_NUMBER,
      LDAP_USER_TITLE,
      LDAP_USER_UID,
      LDAP_USER_UID_NUMBER,
      LDAP_USER_UNIX_HOME_DIRECTORY,
      LDAP_USER_USER_ACCOUNT_CONTROL,
      LDAP_USER_USER_PRINCIPAL_NAME
  };

}
