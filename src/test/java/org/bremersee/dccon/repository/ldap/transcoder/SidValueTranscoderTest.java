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

package org.bremersee.dccon.repository.ldap.transcoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.Sid;
import org.junit.jupiter.api.Test;

/**
 * The sid value transcoder test.
 *
 * @author Christian Bremer
 */
class SidValueTranscoderTest {

  /**
   * Encode and decode binary value.
   */
  @Test
  void encodeAndDecodeBinaryValue() {
    DomainControllerProperties properties = new DomainControllerProperties();
    properties.setMaxSystemSidSuffix(999);
    properties.setDefaultSidPrefix("S-1-5-21-");
    SidValueTranscoder transcoder = new SidValueTranscoder(properties);

    String expected = "S-1-5-21-2180863875-316980752-2664318681-500";
    byte[] bytes = transcoder.encodeBinaryValue(Sid.builder().value(expected).build());
    assertNotNull(bytes);
    Sid actual = transcoder.decodeBinaryValue(bytes);
    assertNotNull(actual);
    assertEquals(expected, actual.getValue());
    assertTrue(actual.getSystemEntity());

    expected = "S-1-5-21-2180863875-316980752-2664318683-1101";
    bytes = transcoder.encodeBinaryValue(Sid.builder().value(expected).build());
    assertNotNull(bytes);
    actual = transcoder.decodeBinaryValue(bytes);
    assertNotNull(actual);
    assertEquals(expected, actual.getValue());
    assertFalse(actual.getSystemEntity());

    expected = "S-1-5-22-2180863875-316980752-2664318683-1101";
    bytes = transcoder.encodeBinaryValue(Sid.builder().value(expected).build());
    assertNotNull(bytes);
    actual = transcoder.decodeBinaryValue(bytes);
    assertNotNull(actual);
    assertEquals(expected, actual.getValue());
    assertTrue(actual.getSystemEntity());
  }

  /**
   * Gets type.
   */
  @Test
  void getType() {
    assertEquals(Sid.class, new SidValueTranscoder(new DomainControllerProperties()).getType());
  }
}