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
 * The domain group ldap constants.
 *
 * @author Christian Bremer
 */
public interface DomainGroupRepositoryConstants extends RepositoryConstants {

  String LDAP_GROUP_TYPE = "groupType";

  String LDAP_GROUP_MEMBER = "member";

  String[] LDAP_GROUP_BINARY_ATTRIBUTES = {
      LDAP_OBJECT_SID
  };

  String[] LDAP_GROUP_MAPPED_ATTRIBUTES = {
      LDAP_WHEN_CREATED,
      LDAP_WHEN_CHANGED,
      LDAP_GROUP_TYPE,
      LDAP_DESCRIPTION,
      LDAP_GID_NUMBER,
      LDAP_MAIL,
      LDAP_GROUP_MEMBER,
      LDAP_MEMBER_OF_GROUP,
      LDAP_NIS_DOMAIN,
      LDAP_OBJECT_SID,
      LDAP_SAM_ACCOUNT_NAME
  };

}
