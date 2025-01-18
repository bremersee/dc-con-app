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
import static org.bremersee.ldaptive.LdaptiveEntryMapper.getAttributeValue;

import org.bremersee.dccon.model.CommonAttributes;
import org.bremersee.dccon.repository.RepositoryConstants;
import org.bremersee.dccon.repository.transcoder.GeneralizedTimeToOffsetDateTimeValueTranscoder;
import org.bremersee.ldaptive.LdaptiveTemplate;
import org.ldaptive.LdapEntry;
import org.ldaptive.SearchRequest;
import org.ldaptive.dn.Dn;

/**
 * The interface CommonAttributesLdapMapper.
 *
 * @author Christian Bremer
 */
public interface CommonAttributesLdapMapper extends RepositoryConstants {

  GeneralizedTimeToOffsetDateTimeValueTranscoder WHEN_TIME_VALUE_TRANSCODER
      = new GeneralizedTimeToOffsetDateTimeValueTranscoder();

  static void mapCommonAttributes(
      LdaptiveTemplate ldaptiveTemplate,
      Dn dn,
      CommonAttributes commonAttributes) {

    if (isNull(ldaptiveTemplate) || isNull(dn) || dn.isEmpty() || isNull(commonAttributes)) {
      return;
    }
    String[] returnAttributes = new String[]{LDAP_WHEN_CREATED, LDAP_WHEN_CHANGED};
    SearchRequest searchRequest = SearchRequest
        .objectScopeSearchRequest(dn.format(), returnAttributes);
    ldaptiveTemplate.findOne(searchRequest)
        .ifPresent(ldapEntry -> mapCommonAttributes(ldapEntry, commonAttributes));
  }

  /**
   * Map common attributes.
   *
   * @param source the source
   * @param destination the destination
   */
  static void mapCommonAttributes(
      LdapEntry source,
      CommonAttributes destination) {

    if (isNull(source) || isNull(destination)) {
      return;
    }
    destination.setCreated(
        getAttributeValue(source, LDAP_WHEN_CREATED, WHEN_TIME_VALUE_TRANSCODER, null));
    destination.setDistinguishedName(source.getDn());
    destination.setModified(
        getAttributeValue(source, LDAP_WHEN_CHANGED, WHEN_TIME_VALUE_TRANSCODER, null));
  }
}
