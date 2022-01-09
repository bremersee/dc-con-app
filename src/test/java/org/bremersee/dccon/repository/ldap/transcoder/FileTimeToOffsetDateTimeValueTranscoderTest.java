package org.bremersee.dccon.repository.ldap.transcoder;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The file time value transcoder test.
 *
 * @author Christian Bremer
 */
@ExtendWith(SoftAssertionsExtension.class)
class FileTimeToOffsetDateTimeValueTranscoderTest {

  private static final FileTimeToOffsetDateTimeValueTranscoder trancoder
      = new FileTimeToOffsetDateTimeValueTranscoder();

  private static final OffsetDateTime dateTime = OffsetDateTime.parse(
      "2019-12-31T23:59:50Z",
      DateTimeFormatter.ISO_OFFSET_DATE_TIME);

  private static final String ldapValue = "132223103900000000";

  /**
   * Decode string value.
   *
   * @param softly the soft assertions
   */
  @Test
  void decodeStringValue(SoftAssertions softly) {
    softly.assertThat(trancoder.decodeStringValue(null)).isNull();
    softly.assertThat(trancoder.decodeStringValue("")).isNull();
    softly.assertThat(trancoder.decodeStringValue("0")).isNull();

    OffsetDateTime actual = trancoder.decodeStringValue(ldapValue);
    softly.assertThat(actual)
        .isEqualTo(dateTime);
  }

  /**
   * Encode string value.
   *
   * @param softly the soft assertions
   */
  @Test
  void encodeStringValue(SoftAssertions softly) {
    softly.assertThat(trancoder.encodeStringValue(null)).isNull();
    String actual = trancoder.encodeStringValue(dateTime);
    softly.assertThat(actual)
        .isEqualTo(ldapValue);
  }

  /**
   * Gets type.
   */
  @Test
  void getType() {
    assertThat(trancoder.getType())
        .isEqualTo(OffsetDateTime.class);
  }
}