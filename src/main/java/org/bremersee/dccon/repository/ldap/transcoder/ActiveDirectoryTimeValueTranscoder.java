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
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import lombok.extern.slf4j.Slf4j;
import org.ldaptive.io.AbstractStringValueTranscoder;
import org.springframework.util.StringUtils;

/**
 * The active directory time value transcoder.
 *
 * @author Christian Bremer
 */
@Slf4j
public class ActiveDirectoryTimeValueTranscoder extends
    AbstractStringValueTranscoder<OffsetDateTime> {

  private static final long ACTIVE_DIRECTORY_START_TIME;

  static {
    final GregorianCalendar cal = new GregorianCalendar();
    cal.setTimeZone(TimeZone.getTimeZone("GMT"));
    cal.set(1601, Calendar.JANUARY, 1, 0, 0, 0);
    cal.set(Calendar.MILLISECOND, 0);
    ACTIVE_DIRECTORY_START_TIME = cal.getTimeInMillis();
  }

  @Override
  public OffsetDateTime decodeStringValue(String value) {
    if (!StringUtils.hasText(value) || "0".equals(value)) {
      return null;
    }
    try {
      return OffsetDateTime.ofInstant(
          new Date(ACTIVE_DIRECTORY_START_TIME + (Long.parseLong(value) / 10000L)).toInstant(),
          ZoneOffset.UTC);

    } catch (final Exception e) {
      log.error("Active directory time value [{}] could not be parsed.", value, e);
      return null;
    }
  }

  @Override
  public String encodeStringValue(OffsetDateTime value) {
    if (value == null) {
      return null;
    }
    final long millis = Date.from(value.toInstant()).getTime();
    return String.valueOf(10000L * (millis - ACTIVE_DIRECTORY_START_TIME));
  }

  @Override
  public Class<OffsetDateTime> getType() {
    return OffsetDateTime.class;
  }
}
