/*
 * Copyright 2016 the original author or authors.
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

package org.bremersee.dccon.business;

import javax.validation.constraints.NotNull;
import org.bremersee.dccon.model.SambaGroup;
import org.bremersee.dccon.model.SambaGroupItem;
import org.bremersee.dccon.model.SambaUser;
import org.ldaptive.LdapEntry;

/**
 * The interface Ldap entry mapper.
 *
 * @author Christian Bremer
 */
public interface LdapEntryMapper {

  /**
   * Map ldap entry to samba group item.
   *
   * @param ldapEntry the ldap entry
   * @return the samba group item
   */
  SambaGroupItem mapLdapEntryToSambaGroupItem(@NotNull LdapEntry ldapEntry);

  /**
   * Map ldap entry to samba group.
   *
   * @param ldapEntry the ldap entry
   * @return the samba group
   */
  SambaGroup mapLdapEntryToSambaGroup(@NotNull LdapEntry ldapEntry);

  /**
   * Map ldap entry to samba user.
   *
   * @param ldapEntry the ldap entry
   * @return the samba user
   */
  SambaUser mapLdapEntryToSambaUser(@NotNull LdapEntry ldapEntry);

}
