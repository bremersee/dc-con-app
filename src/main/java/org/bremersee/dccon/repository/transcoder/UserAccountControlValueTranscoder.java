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

import lombok.extern.slf4j.Slf4j;
import org.ldaptive.transcode.AbstractStringValueTranscoder;
import org.ldaptive.transcode.IntegerValueTranscoder;

/**
 * The user account control value transcoder.
 *
 * @author Christian Bremer
 */
@Slf4j
public class UserAccountControlValueTranscoder
    extends AbstractStringValueTranscoder<UserAccountControl> {

  private final IntegerValueTranscoder intValueTranscoder = new IntegerValueTranscoder();

  @Override
  public UserAccountControl decodeStringValue(String value) {
    try {
      return new UserAccountControl(intValueTranscoder.decodeStringValue(value));
    } catch (RuntimeException re) {
      return new UserAccountControl();
    }
  }

  @Override
  public String encodeStringValue(UserAccountControl value) {
    return intValueTranscoder.encodeStringValue(value.getValue());
  }

  @Override
  public Class<UserAccountControl> getType() {
    return UserAccountControl.class;
  }
}
