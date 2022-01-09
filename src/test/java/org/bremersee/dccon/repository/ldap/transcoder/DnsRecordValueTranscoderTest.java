package org.bremersee.dccon.repository.ldap.transcoder;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.bremersee.dccon.model.DnsRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The dns record value transcoder test.
 *
 * @author Christian Bremer
 */
@ExtendWith(SoftAssertionsExtension.class)
class DnsRecordValueTranscoderTest {

  private static final DnsRecordValueTranscoder transcoder = new DnsRecordValueTranscoder();

  private static final String A_RECORD_BASE64 = "BAABAAXwAAAmWwAAAAADhAAAAAD46jcAwKgBKQ==";
  private static final String A_RECORD_TYPE = "A";
  private static final String A_RECORD_VALUE = "192.168.1.41";
  private static final Integer A_RECORD_VERSION = 5;
  private static final Integer A_RECORD_SERIAL = 23334;
  private static final Integer A_RECORD_TTL = 900;
  private static final OffsetDateTime A_RECORD_TIME_STAMP = OffsetDateTime.parse(
      "2019-01-23T00:00:00Z", DateTimeFormatter.ISO_OFFSET_DATE_TIME);

  private static final String CNAME_RECORD_BASE64
      = "GQAFAAXwAAB5HwAAAAADhAAAAADy4TcAFwQCbGIEZWl4ZQlicmVtZXJzZWUDb3JnAA==";
  private static final String CNAME_RECORD_TYPE = "CNAME";
  private static final String CNAME_RECORD_VALUE = "lb.eixe.bremersee.org";
  private static final Integer CNAME_RECORD_VERSION = 5;
  private static final Integer CNAME_RECORD_SERIAL = 8057;
  private static final Integer CNAME_RECORD_TTL = 900;
  private static final OffsetDateTime CNAME_RECORD_TIME_STAMP = OffsetDateTime.parse(
      "2018-10-18T18:00:00Z", DateTimeFormatter.ISO_OFFSET_DATE_TIME);

  private static final String PTR_RECORD_BASE64
      = "IAAMAAXwAACeAAAAAAADhAAAAADQ3TcAHgQJdGstMjZhLWUwBGVpeGUJYnJlbWVyc2VlA29yZwA=";
  private static final String PTR_RECORD_TYPE = "PTR";
  private static final String PTR_RECORD_VALUE = "tk-26a-e0.eixe.bremersee.org";
  private static final Integer PTR_RECORD_VERSION = 5;
  private static final Integer PTR_RECORD_SERIAL = 158;
  private static final Integer PTR_RECORD_TTL = 900;
  private static final OffsetDateTime PTR_RECORD_TIME_STAMP = OffsetDateTime.parse(
      "2018-09-04T16:00:00Z", DateTimeFormatter.ISO_OFFSET_DATE_TIME);

  private static final String UNKNOWN_RECORD_BASE64
      = "CAAAAAUAAACSWgAAAAAAAAAAAAAAAAAA0iISorCx1AE=";
  private static final String UNKNOWN_RECORD_TYPE = "UNKNOWN";
  private static final String UNKNOWN_RECORD_VALUE = "D22212A2B0B1D401";
  private static final Integer UNKNOWN_RECORD_VERSION = 5;
  private static final Integer UNKNOWN_RECORD_SERIAL = 23186;
  private static final Integer UNKNOWN_RECORD_TTL = 0;
  private static final OffsetDateTime UNKNOWN_RECORD_TIME_STAMP = OffsetDateTime.parse(
      "1601-01-01T00:00:00Z", DateTimeFormatter.ISO_OFFSET_DATE_TIME);

  /**
   * Decode binary value record a.
   *
   * @param softly the soft assertions
   */
  @Test
  void decodeBinaryValueRecordA(SoftAssertions softly) {
    DnsRecord actual = transcoder.decodeBinaryValue(Base64.getDecoder().decode(A_RECORD_BASE64));
    softly.assertThat(actual)
        .isNotNull();
    softly.assertThat(actual)
        .extracting(DnsRecord::getRecordType)
        .isEqualTo(A_RECORD_TYPE);
    softly.assertThat(actual)
        .extracting(DnsRecord::getRecordValue)
        .isEqualTo(A_RECORD_VALUE);
    softly.assertThat(actual)
        .extracting(DnsRecord::getVersion)
        .isEqualTo(A_RECORD_VERSION);
    softly.assertThat(actual)
        .extracting(DnsRecord::getSerial)
        .isEqualTo(A_RECORD_SERIAL);
    softly.assertThat(actual)
        .extracting(DnsRecord::getTtlSeconds)
        .isEqualTo(A_RECORD_TTL);
    softly.assertThat(actual)
        .extracting(DnsRecord::getTimeStamp)
        .isEqualTo(A_RECORD_TIME_STAMP);
  }

  /**
   * Decode binary value record cname.
   *
   * @param softly the soft assertions
   */
  @Test
  void decodeBinaryValueRecordCname(SoftAssertions softly) {
    DnsRecord actual = transcoder
        .decodeBinaryValue(Base64.getDecoder().decode(CNAME_RECORD_BASE64));
    softly.assertThat(actual)
        .isNotNull();
    softly.assertThat(actual)
        .extracting(DnsRecord::getRecordType)
        .isEqualTo(CNAME_RECORD_TYPE);
    softly.assertThat(actual)
        .extracting(DnsRecord::getRecordValue)
        .isEqualTo(CNAME_RECORD_VALUE);
    softly.assertThat(actual)
        .extracting(DnsRecord::getVersion)
        .isEqualTo(CNAME_RECORD_VERSION);
    softly.assertThat(actual)
        .extracting(DnsRecord::getSerial)
        .isEqualTo(CNAME_RECORD_SERIAL);
    softly.assertThat(actual)
        .extracting(DnsRecord::getTtlSeconds)
        .isEqualTo(CNAME_RECORD_TTL);
    softly.assertThat(actual)
        .extracting(DnsRecord::getTimeStamp)
        .isEqualTo(CNAME_RECORD_TIME_STAMP);
  }

  /**
   * Decode binary value record ptr.
   *
   * @param softly the soft assertions
   */
  @Test
  void decodeBinaryValueRecordPtr(SoftAssertions softly) {
    DnsRecord actual = transcoder
        .decodeBinaryValue(Base64.getDecoder().decode(PTR_RECORD_BASE64));
    softly.assertThat(actual)
        .isNotNull();
    softly.assertThat(actual)
        .extracting(DnsRecord::getRecordType)
        .isEqualTo(PTR_RECORD_TYPE);
    softly.assertThat(actual)
        .extracting(DnsRecord::getRecordValue)
        .isEqualTo(PTR_RECORD_VALUE);
    softly.assertThat(actual)
        .extracting(DnsRecord::getVersion)
        .isEqualTo(PTR_RECORD_VERSION);
    softly.assertThat(actual)
        .extracting(DnsRecord::getSerial)
        .isEqualTo(PTR_RECORD_SERIAL);
    softly.assertThat(actual)
        .extracting(DnsRecord::getTtlSeconds)
        .isEqualTo(PTR_RECORD_TTL);
    softly.assertThat(actual)
        .extracting(DnsRecord::getTimeStamp)
        .isEqualTo(PTR_RECORD_TIME_STAMP);
  }

  /**
   * Decode binary value record unknown.
   *
   * @param softly the soft assertions
   */
  @Test
  void decodeBinaryValueRecordUnknown(SoftAssertions softly) {
    DnsRecord actual = transcoder
        .decodeBinaryValue(Base64.getDecoder().decode(UNKNOWN_RECORD_BASE64));
    softly.assertThat(actual)
        .isNotNull();
    softly.assertThat(actual)
        .extracting(DnsRecord::getRecordType)
        .isEqualTo(UNKNOWN_RECORD_TYPE);
    softly.assertThat(actual)
        .extracting(DnsRecord::getRecordValue)
        .isEqualTo(UNKNOWN_RECORD_VALUE);
    softly.assertThat(actual)
        .extracting(DnsRecord::getVersion)
        .isEqualTo(UNKNOWN_RECORD_VERSION);
    softly.assertThat(actual)
        .extracting(DnsRecord::getSerial)
        .isEqualTo(UNKNOWN_RECORD_SERIAL);
    softly.assertThat(actual)
        .extracting(DnsRecord::getTtlSeconds)
        .isEqualTo(UNKNOWN_RECORD_TTL);
    softly.assertThat(actual)
        .extracting(DnsRecord::getTimeStamp)
        .isEqualTo(UNKNOWN_RECORD_TIME_STAMP);
  }

  /**
   * Encode binary value.
   *
   * @param softly the soft assertions
   */
  @Test
  void encodeBinaryValue(SoftAssertions softly) {
    DnsRecord dnsRecord = new DnsRecord();
    softly.assertThat(transcoder.encodeBinaryValue(dnsRecord))
        .isNull();

    dnsRecord.setRecordRawValue(Base64.getDecoder().decode(A_RECORD_BASE64));
    softly.assertThat(transcoder.encodeBinaryValue(dnsRecord))
        .containsExactly(Base64.getDecoder().decode(A_RECORD_BASE64));
  }

  /**
   * Gets type.
   */
  @Test
  void getType() {
    assertThat(transcoder.getType())
        .isEqualTo(DnsRecord.class);
  }
}