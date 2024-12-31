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

package org.bremersee.dccon.repository.transcoder;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.model.Sid;
import org.ldaptive.ad.SecurityIdentifier;
import org.ldaptive.transcode.AbstractBinaryValueTranscoder;

/**
 * The SID value transcoder.
 *
 * @author Christian Bremer
 */
@Slf4j
public class SidValueTranscoder extends AbstractBinaryValueTranscoder<Sid> {

  private static final String DEFAULT_SID_PREFIX = "S-1-5-21-";

  private static final int MAX_SYSTEM_SID_SUFFIX = 999;

  @Override
  public Sid decodeBinaryValue(byte[] value) {
    return Optional.ofNullable(value)
        .map(SecurityIdentifier::toString)
        .map(objectSid -> Sid.builder()
            .value(objectSid)
            .systemEntity(isSystemEntity(objectSid))
            .build())
        .orElse(null);
  }

  @Override
  public byte[] encodeBinaryValue(Sid value) {
    return Optional.ofNullable(value)
        .map(Sid::getValue)
        .map(SecurityIdentifier::toBytes)
        .orElse(null);
  }

  @Override
  public Class<Sid> getType() {
    return Sid.class;
  }

  private boolean isSystemEntity(final String objectSid) {
    if (!objectSid.startsWith(DEFAULT_SID_PREFIX)) {
      return true;
    }
    final int index = objectSid.lastIndexOf('-');
    if (index > -1) {
      try {
        return MAX_SYSTEM_SID_SUFFIX >= Integer.parseInt(objectSid.substring(index + 1));
      } catch (RuntimeException ignored) {
        // ignored
      }
    }
    return false;
  }
}
