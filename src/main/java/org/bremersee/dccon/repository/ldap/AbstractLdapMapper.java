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

package org.bremersee.dccon.repository.ldap;

import static org.bremersee.data.ldaptive.LdaptiveEntryMapper.getAttributeValue;

import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.CommonAttributes;
import org.bremersee.dccon.repository.ldap.transcoder.GeneralizedTimeToOffsetDateTimeValueTranscoder;
import org.ldaptive.LdapEntry;

/**
 * The abstract ldap mapper.
 *
 * @author Christian Bremer
 */
abstract class AbstractLdapMapper {

  static final String WHEN_CREATED = "whenCreated";

  static final String WHEN_CHANGED = "whenChanged";

  private static final GeneralizedTimeToOffsetDateTimeValueTranscoder WHEN_TIME_VALUE_TRANSCODER
      = new GeneralizedTimeToOffsetDateTimeValueTranscoder();

  @Getter(AccessLevel.PROTECTED)
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
   * Create distinguished name.
   *
   * @param rdn the rdn
   * @param rdnValue the rdn value
   * @param baseDn the base dn
   * @return the string
   */
  String createDn(
      @NotNull final String rdn,
      @NotNull final String rdnValue,
      @NotNull final String baseDn) {
    return rdn + "=" + rdnValue + "," + baseDn;
  }

  /**
   * Map common attributes.
   *
   * @param source the source
   * @param destination the destination
   */
  void mapCommonAttributes(final LdapEntry source, final CommonAttributes destination) {
    if (source != null && destination != null) {
      destination.setCreated(getAttributeValue(
          source, WHEN_CREATED, WHEN_TIME_VALUE_TRANSCODER, null));
      destination.setDistinguishedName(source.getDn());
      destination.setModified(getAttributeValue(
          source, WHEN_CHANGED, WHEN_TIME_VALUE_TRANSCODER, null));
    }
  }

}
