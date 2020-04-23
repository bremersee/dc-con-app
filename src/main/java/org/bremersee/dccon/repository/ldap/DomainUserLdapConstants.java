/*
 * Copyright 2019 the original author or authors.
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

package org.bremersee.dccon.repository.ldap;

/**
 * @author Christian Bremer
 */
public class DomainUserLdapConstants {

  public static final String DESCRIPTION = "description";

  public static final String DISPLAY_NAME = "displayName";

  public static final String GECOS = "gecos";

  public static final String GIVEN_NAME = "givenName";

  public static final String HOME_DIRECTORY = "homeDirectory";

  public static final String JPEG_PHOTO = "jpegPhoto";

  public static final String LAST_LOGON = "lastLogon";

  public static final String LOGIN_SHELL = "loginShell";

  public static final String LOGON_COUNT = "logonCount";

  public static final String MAIL = "mail";

  public static final String MEMBER_OF = "memberOf";

  public static final String MOBILE = "mobile";

  public static final String NAME = "name";

  public static final String OBJECT_SID = "objectSid";

  public static final String PWD_LAST_SET = "pwdLastSet";

  public static final String SAM_ACCOUNT_NAME = "sAMAccountName";

  public static final String SN = "sn";

  public static final String TELEPHONE_NUMBER = "telephoneNumber";

  public static final String UID = "uid";

  public static final String UNIX_HOME_DIRECTORY = "unixHomeDirectory";

  public static final String USER_ACCOUNT_CONTROL = "userAccountControl";

  public static final String[] BINARY_ATTRIBUTES = {
      JPEG_PHOTO,
      OBJECT_SID
  };

  private DomainUserLdapConstants() {
  }
}
