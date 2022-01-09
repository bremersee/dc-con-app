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

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.Sid;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The sid value transcoder test.
 *
 * @author Christian Bremer
 */
@ExtendWith(SoftAssertionsExtension.class)
class SidValueTranscoderTest {

  /**
   * Encode and decode binary value.
   *
   * @param softly the soft assertions
   */
  @Test
  void encodeAndDecodeBinaryValue(SoftAssertions softly) {
    DomainControllerProperties properties = new DomainControllerProperties();
    properties.setMaxSystemSidSuffix(999);
    properties.setDefaultSidPrefix("S-1-5-21-");
    SidValueTranscoder transcoder = new SidValueTranscoder(properties);

    String expected = "S-1-5-21-2180863875-316980752-2664318681-500";
    byte[] bytes = transcoder.encodeBinaryValue(Sid.builder().value(expected).build());
    softly.assertThat(bytes).isNotNull();
    Sid actual = transcoder.decodeBinaryValue(bytes);
    softly.assertThat(actual)
        .isNotNull();
    softly.assertThat(actual)
        .extracting(Sid::getValue)
        .isEqualTo(expected);
    softly.assertThat(actual)
        .extracting(Sid::getSystemEntity)
        .isEqualTo(Boolean.TRUE);

    expected = "S-1-5-21-2180863875-316980752-2664318683-1101";
    bytes = transcoder.encodeBinaryValue(Sid.builder().value(expected).build());
    softly.assertThat(bytes).isNotNull();
    actual = transcoder.decodeBinaryValue(bytes);
    softly.assertThat(actual)
        .isNotNull();
    softly.assertThat(actual)
        .extracting(Sid::getValue)
        .isEqualTo(expected);
    softly.assertThat(actual)
        .extracting(Sid::getSystemEntity)
        .isEqualTo(Boolean.FALSE);

    expected = "S-1-5-22-2180863875-316980752-2664318683-1101";
    bytes = transcoder.encodeBinaryValue(Sid.builder().value(expected).build());
    softly.assertThat(bytes).isNotNull();
    actual = transcoder.decodeBinaryValue(bytes);
    softly.assertThat(actual)
        .isNotNull();
    softly.assertThat(actual)
        .extracting(Sid::getValue)
        .isEqualTo(expected);
    softly.assertThat(actual)
        .extracting(Sid::getSystemEntity)
        .isEqualTo(Boolean.TRUE);
  }

  /**
   * Gets type.
   */
  @Test
  void getType() {
    Class<?> actual = new SidValueTranscoder(new DomainControllerProperties()).getType();
    assertThat(actual)
        .isEqualTo(Sid.class);
  }
}