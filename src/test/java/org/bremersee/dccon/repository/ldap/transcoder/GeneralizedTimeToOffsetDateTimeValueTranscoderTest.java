package org.bremersee.dccon.repository.ldap.transcoder;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Month;
import java.time.OffsetDateTime;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The generalized time value transcoder test.
 *
 * @author Christian Bremer
 */
@ExtendWith(SoftAssertionsExtension.class)
class GeneralizedTimeToOffsetDateTimeValueTranscoderTest {

  private static final GeneralizedTimeToOffsetDateTimeValueTranscoder transcoder
      = new GeneralizedTimeToOffsetDateTimeValueTranscoder();

  private static final String ldapValue = "20191226154554.000Z";

  /**
   * Decode and encode.
   *
   * @param softly the soft assertions
   */
  @Test
  void decodeAndEncode(SoftAssertions softly) {
    OffsetDateTime dateTime = transcoder.decodeStringValue(ldapValue);
    softly.assertThat(dateTime)
        .isNotNull();
    softly.assertThat(dateTime.getYear())
        .isEqualTo(2019);
    softly.assertThat(dateTime.getMonth())
        .isEqualTo(Month.DECEMBER);
    softly.assertThat(dateTime.getDayOfMonth())
        .isEqualTo(26);
    softly.assertThat(dateTime.getHour())
        .isEqualTo(15);
    softly.assertThat(dateTime.getMinute())
        .isEqualTo(45);
    softly.assertThat(dateTime.getSecond())
        .isEqualTo(54);
    softly.assertThat(transcoder.encodeStringValue(dateTime))
        .isEqualTo(ldapValue);
  }

  /**
   * Gets type.
   */
  @Test
  void getType() {
    assertThat(transcoder.getType())
        .isEqualTo(OffsetDateTime.class);
  }
}