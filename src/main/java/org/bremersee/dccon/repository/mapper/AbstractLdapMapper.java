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

package org.bremersee.dccon.repository.mapper;

import static org.bremersee.ldaptive.LdaptiveEntryMapper.getAttributeValue;

import lombok.AccessLevel;
import lombok.Getter;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.CommonAttributes;
import org.bremersee.dccon.repository.RepositoryConstants;
import org.bremersee.dccon.repository.transcoder.GeneralizedTimeToOffsetDateTimeValueTranscoder;
import org.ldaptive.LdapEntry;

/**
 * The abstract ldap mapper.
 *
 * @author Christian Bremer
 */
abstract class AbstractLdapMapper implements RepositoryConstants {

  private static final GeneralizedTimeToOffsetDateTimeValueTranscoder WHEN_TIME_VALUE_TRANSCODER
      = new GeneralizedTimeToOffsetDateTimeValueTranscoder();

  @Getter(AccessLevel.PACKAGE)
  private final DomainControllerProperties properties;

  /**
   * Instantiates a new abstract ldap mapper.
   *
   * @param properties the properties
   */
  AbstractLdapMapper(DomainControllerProperties properties) {
    this.properties = properties;
  }

  /**
   * Map common attributes.
   *
   * @param source the source
   * @param destination the destination
   */
  void mapCommonAttributes(LdapEntry source, CommonAttributes destination) {
    if (source != null && destination != null) {
      destination.setCreated(
          getAttributeValue(source, LDAP_WHEN_CREATED, WHEN_TIME_VALUE_TRANSCODER, null));
      destination.setDistinguishedName(source.getDn());
      destination.setModified(
          getAttributeValue(source, LDAP_WHEN_CHANGED, WHEN_TIME_VALUE_TRANSCODER, null));
    }
  }

}
