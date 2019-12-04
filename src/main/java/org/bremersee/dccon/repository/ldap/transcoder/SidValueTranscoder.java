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

package org.bremersee.dccon.repository.ldap.transcoder;

import java.util.Optional;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.Sid;
import org.ldaptive.io.AbstractStringValueTranscoder;

/**
 * The SID value transcoder.
 *
 * @author Christian Bremer
 */
public class SidValueTranscoder extends AbstractStringValueTranscoder<Sid> {

  private DomainControllerProperties properties;

  /**
   * Instantiates a new Sid value transcoder.
   *
   * @param properties the properties
   */
  public SidValueTranscoder(DomainControllerProperties properties) {
    this.properties = properties;
  }

  @Override
  public Sid decodeStringValue(String value) {
    return Optional.ofNullable(value)
        .map(objectSid -> Sid.builder()
            .value(value)
            .systemEntity(isSystemEntity(value))
            .build())
        .orElse(null);
  }

  @Override
  public String encodeStringValue(Sid value) {
    return Optional.ofNullable(value)
        .map(Sid::getValue)
        .orElse(null);
  }

  @Override
  public Class<Sid> getType() {
    return Sid.class;
  }

  private boolean isSystemEntity(final String objectSid) {
    if (!objectSid.startsWith(properties.getDefaultSidPrefix())) {
      return true;
    }
    final int index = objectSid.lastIndexOf('-');
    if (index > -1) {
      try {
        return properties
            .getMaxSystemSidSuffix() <= Integer.parseInt(objectSid.substring(index + 1));
      } catch (Exception ignored) {
        // ignored
      }
    }
    return false;
  }
}
