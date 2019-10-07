package org.bremersee.dccon.repository.ldap.transcoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.Test;

/**
 * The file time value transcoder test.
 *
 * @author Christian Bremer
 */
public class FileTimeToOffsetDateTimeValueTranscoderTest {

  private static final FileTimeToOffsetDateTimeValueTranscoder trancoder
      = new FileTimeToOffsetDateTimeValueTranscoder();

  private static final OffsetDateTime dateTime = OffsetDateTime.parse(
      "2019-12-31T23:59:50Z",
      DateTimeFormatter.ISO_OFFSET_DATE_TIME);

  private static final String ldapValue = "132223103900000000";

  /**
   * Decode string value.
   */
  @Test
  public void decodeStringValue() {
    assertNull(trancoder.decodeStringValue(null));
    assertNull(trancoder.decodeStringValue(""));
    assertNull(trancoder.decodeStringValue("0"));
    final OffsetDateTime actual = trancoder.decodeStringValue(ldapValue);
    assertEquals(dateTime, actual);
  }

  /**
   * Encode string value.
   */
  @Test
  public void encodeStringValue() {
    assertNull(trancoder.encodeStringValue(null));
    final String actual = trancoder.encodeStringValue(dateTime);
    assertEquals(ldapValue, actual);
  }

  /**
   * Gets type.
   */
  @Test
  public void getType() {
    assertEquals(OffsetDateTime.class, trancoder.getType());
  }
}