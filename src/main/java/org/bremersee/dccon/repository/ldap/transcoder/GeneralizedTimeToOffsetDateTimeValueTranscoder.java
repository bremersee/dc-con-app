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

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.ldaptive.io.AbstractStringValueTranscoder;
import org.ldaptive.io.GeneralizedTimeValueTranscoder;
import org.springframework.util.StringUtils;

/**
 * The generalized time value transcoder.
 *
 * @author Christian Bremer
 */
public class GeneralizedTimeToOffsetDateTimeValueTranscoder
    extends AbstractStringValueTranscoder<OffsetDateTime> {

  private static final GeneralizedTimeValueTranscoder transcoder
      = new GeneralizedTimeValueTranscoder();

  @Override
  public OffsetDateTime decodeStringValue(String value) {
    return Optional.ofNullable(value)
        .filter(StringUtils::hasText)
        .map(transcoder::decodeStringValue)
        .map(ZonedDateTime::toOffsetDateTime)
        .orElse(null);
  }

  @Override
  public String encodeStringValue(OffsetDateTime value) {
    return Optional.ofNullable(value)
        .map(OffsetDateTime::toZonedDateTime)
        .map(transcoder::encodeStringValue)
        .orElse(null);
  }

  @Override
  public Class<OffsetDateTime> getType() {
    return OffsetDateTime.class;
  }
}
