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

/**
 * The organisation unit constants.
 *
 * @author Christian Bremer
 */
public interface OrganizationalUnitRepositoryConstants extends RepositoryConstants {

  String[] LDAP_OU_BINARY_ATTRIBUTES = {};

  String[] LDAP_OU_MAPPED_ATTRIBUTES = {
      LDAP_WHEN_CREATED,
      LDAP_WHEN_CHANGED,
      LDAP_DESCRIPTION,
      LDAP_IS_CRITICAL_SYSTEM_OBJECT,
      LDAP_NAME
  };

}
