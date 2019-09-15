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

import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.TimeZone;
import lombok.extern.slf4j.Slf4j;
import org.ldaptive.io.AbstractStringValueTranscoder;
import org.springframework.util.StringUtils;

/**
 * The when time value transcoder.
 *
 * @author Christian Bremer
 */
@Slf4j
public class WhenTimeValueTranscoder extends AbstractStringValueTranscoder<OffsetDateTime> {

  private static final String WHEN_DATE_PATTERN = "yyyyMMddHHmmss"; // 20180520150034.0Z

  private static final SimpleDateFormat WHEN_DATE_FORMAT = new SimpleDateFormat(WHEN_DATE_PATTERN);

  static {
    WHEN_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  @Override
  public OffsetDateTime decodeStringValue(String value) {
    if (!StringUtils.hasText(value) || value.length() < WHEN_DATE_PATTERN.length()) {
      return null;
    }
    try {
      return OffsetDateTime.ofInstant(
          WHEN_DATE_FORMAT.parse(value.substring(0, WHEN_DATE_PATTERN.length())).toInstant(),
          ZoneOffset.UTC);

    } catch (final Exception e) {
      log.error("Parsing when time [{}] failed. Returning null.", value, e);
      return null;
    }
  }

  @Override
  public String encodeStringValue(OffsetDateTime value) {
    return value != null ? WHEN_DATE_FORMAT.format(value) : null;
  }

  @Override
  public Class<OffsetDateTime> getType() {
    return OffsetDateTime.class;
  }
}
