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
import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.model.DomainGroupItem;
import org.bremersee.dccon.model.DomainUser;
import org.ldaptive.LdapEntry;

/**
 * The interface Ldap entry mapper.
 *
 * @author Christian Bremer
 */
public interface LdapEntryMapper {

  /**
   * Map ldap entry to domain group item.
   *
   * @param ldapEntry the ldap entry
   * @return the domain group item
   */
  DomainGroupItem mapLdapEntryToDomainGroupItem(@NotNull LdapEntry ldapEntry);

  /**
   * Map ldap entry to domain group.
   *
   * @param ldapEntry the ldap entry
   * @return the domain group
   */
  DomainGroup mapLdapEntryToDomainGroup(
      @NotNull LdapEntry ldapEntry);

  /**
   * Map ldap entry to domain user.
   *
   * @param ldapEntry the ldap entry
   * @return the domain user
   */
  DomainUser mapLdapEntryToDomainUser(
      @NotNull LdapEntry ldapEntry);

}
