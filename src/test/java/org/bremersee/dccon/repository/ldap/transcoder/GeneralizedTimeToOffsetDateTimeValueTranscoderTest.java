package org.bremersee.dccon.repository.ldap.transcoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Month;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/**
 * The generalized time value transcoder test.
 *
 * @author Christian Bremer
 */
class GeneralizedTimeToOffsetDateTimeValueTranscoderTest {

  private static final GeneralizedTimeToOffsetDateTimeValueTranscoder transcoder
      = new GeneralizedTimeToOffsetDateTimeValueTranscoder();

  private static final String ldapValue = "20191226154554.000Z";

  /**
   * Decode and encode.
   */
  @Test
  void decodeAndEncode() {
    OffsetDateTime dateTime = transcoder.decodeStringValue(ldapValue);
    assertNotNull(dateTime);
    assertEquals(2019, dateTime.getYear());
    assertEquals(Month.DECEMBER, dateTime.getMonth());
    assertEquals(26, dateTime.getDayOfMonth());
    assertEquals(15, dateTime.getHour());
    assertEquals(45, dateTime.getMinute());
    assertEquals(54, dateTime.getSecond());

    assertEquals(ldapValue, transcoder.encodeStringValue(dateTime));
  }

  /**
   * Gets type.
   */
  @Test
  void getType() {
    assertEquals(OffsetDateTime.class, transcoder.getType());
  }
}