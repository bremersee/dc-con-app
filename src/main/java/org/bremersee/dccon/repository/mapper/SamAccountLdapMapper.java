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

package org.bremersee.dccon.repository.mapper;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.bremersee.dccon.repository.mapper.CommonAttributesLdapMapper.mapCommonAttributes;
import static org.bremersee.ldaptive.LdaptiveEntryMapper.getAttributeValue;
import static org.bremersee.ldaptive.LdaptiveEntryMapper.getAttributeValuesAsList;
import static org.bremersee.ldaptive.LdaptiveEntryMapper.setAttribute;

import java.util.List;
import java.util.Optional;
import org.bremersee.dccon.model.SamAccount;
import org.bremersee.dccon.model.Sid;
import org.ldaptive.AttributeModification;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;

/**
 * The interface SamAccountLdapMapper.
 *
 * @author Christian Bremer
 */
public interface SamAccountLdapMapper extends CommonAttributesLdapMapper {

  default SamAccount mapSamAccount(LdapEntry source) {
    if (isNull(source)) {
      return null;
    }
    SamAccount samAccount = new SamAccount();
    mapSamAccount(source, samAccount);
    return samAccount;
  }

  default void mapSamAccount(LdapEntry source, SamAccount destination) {
    if (isNull(source) || isNull(destination)) {
      return;
    }
    mapCommonAttributes(source, destination);
    destination.setSamAccountName(
        getAttributeValue(source, LDAP_SAM_ACCOUNT_NAME, STRING_VALUE_TRANSCODER, null));
    Sid sid = getAttributeValue(source, LDAP_OBJECT_SID, SID_VALUE_TRANSCODER, null);
    destination.setSid(sid);
    Integer primaryGroupId = getAttributeValue(source, LDAP_PRIMARY_GROUP_ID, INT_VALUE_TRANSCODER,
        null);
    if (isNull(primaryGroupId) && nonNull(sid)) {
      // samAccount is a domain group
      destination.setPrimaryGroupId(sid.getSuffix());
    } else {
      // samAccount is a domain user or computer
      destination.setPrimaryGroupId(primaryGroupId);
    }
    destination.setMemberships(
        getAttributeValuesAsList(source, LDAP_MEMBER_OF_GROUP, STRING_VALUE_TRANSCODER));
  }

  default void mapSamAccount(SamAccount source, LdapEntry destination,
      List<AttributeModification> modifications) {
    if (isNull(source) || isNull(destination) || isNull(modifications)) {
      return;
    }
    setAttribute(destination, LDAP_SAM_ACCOUNT_NAME, source.getSamAccountName(), false,
        STRING_VALUE_TRANSCODER, modifications);
    if (nonNull(source.getPrimaryGroupId()) && Optional
        .ofNullable(destination.getAttribute(LDAP_PRIMARY_GROUP_ID))
        .map(LdapAttribute::getStringValue)
        .isPresent()) {
      setAttribute(destination, LDAP_PRIMARY_GROUP_ID, source.getPrimaryGroupId(), false,
          INT_VALUE_TRANSCODER, modifications);
    }
  }
}
