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

package org.bremersee.dccon.repository;

import org.bremersee.dccon.repository.transcoder.SidValueTranscoder;
import org.ldaptive.transcode.BooleanValueTranscoder;
import org.ldaptive.transcode.ByteArrayValueTranscoder;
import org.ldaptive.transcode.IntegerValueTranscoder;
import org.ldaptive.transcode.StringValueTranscoder;

/**
 * The repository constants.
 *
 * @author Christian Bremer
 */
public interface RepositoryConstants {

  StringValueTranscoder STRING_VALUE_TRANSCODER = new StringValueTranscoder();

  IntegerValueTranscoder INT_VALUE_TRANSCODER = new IntegerValueTranscoder();

  BooleanValueTranscoder BOOLEAN_VALUE_TRANSCODER = new BooleanValueTranscoder();

  ByteArrayValueTranscoder BYTE_ARRAY_VALUE_TRANSCODER = new ByteArrayValueTranscoder();

  SidValueTranscoder SID_VALUE_TRANSCODER = new SidValueTranscoder();

  String LDAP_OBJECT_CLASS = "objectClass";

  String LDAP_OBJECT_CLASS_COMPUTER = "computer";

  String LDAP_OBJECT_CLASS_GROUP = "group";

  String LDAP_OBJECT_CLASS_OU = "organizationalUnit";

  String LDAP_OBJECT_CLASS_USER = "user";

  String LDAP_DN = "distinguishedName";

  String LDAP_WHEN_CREATED = "whenCreated";

  String LDAP_WHEN_CHANGED = "whenChanged";

  String LDAP_CN = "cn";

  String LDAP_DESCRIPTION = "description";

  /**
   * The constant GID_NUMBER (rfc2307).
   */
  String LDAP_GID_NUMBER = "gidNumber";

  String LDAP_MAIL = "mail";

  String LDAP_MEMBER_OF_GROUP = "memberOf";

  /**
   * The constant NAME. Attribute of the group name.
   */
  String LDAP_NAME = "name";

  /**
   * The constant NIS_DOMAIN (rfc2307).
   */
  String LDAP_NIS_DOMAIN = "msSFU30NisDomain";

  /**
   * The constant NIS_NAME. Attribute of the username / group name (rfc2307).
   */
  String LDAP_NIS_NAME = "msSFU30Name";

  /**
   * The constant OBJECT_SID.
   */
  String LDAP_OBJECT_SID = "objectSid";

  String LDAP_PRIMARY_GROUP_ID = "primaryGroupID";

  /**
   * The constant SAM_ACCOUNT_NAME. Attribute of the group name.
   */
  String LDAP_SAM_ACCOUNT_NAME = "sAMAccountName";

  String LDAP_IS_CRITICAL_SYSTEM_OBJECT = "isCriticalSystemObject";

}
