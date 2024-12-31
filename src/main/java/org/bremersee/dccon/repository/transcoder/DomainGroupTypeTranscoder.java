/*
 * Copyright 2024 the original author or authors.
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

import static java.util.Objects.requireNonNullElse;

import org.bremersee.dccon.model.DomainGroupType;
import org.ldaptive.transcode.AbstractStringValueTranscoder;
import org.ldaptive.transcode.IntegerValueTranscoder;

/**
 * The type DomainGroupTypeTranscoder.
 *
 * @author Christian Bremer
 */
public class DomainGroupTypeTranscoder extends AbstractStringValueTranscoder<DomainGroupType> {

  private static final IntegerValueTranscoder INT_VALUE_TRANSCODER = new IntegerValueTranscoder();

  @Override
  public DomainGroupType decodeStringValue(String value) {
    return DomainGroupType.fromValue(INT_VALUE_TRANSCODER.decodeStringValue(value));
  }

  @Override
  public String encodeStringValue(DomainGroupType value) {
    return String.valueOf(requireNonNullElse(value, DomainGroupType.SECURITY).getValue());
  }

  @Override
  public Class<DomainGroupType> getType() {
    return DomainGroupType.class;
  }
}
