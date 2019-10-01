package org.bremersee.dccon.repository.ldap.transcoder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import org.bremersee.dccon.model.DnsRecord;
import org.junit.Test;

/**
 * The dns record value transcoder test.
 *
 * @author Christian Bremer
 */
public class DnsRecordValueTranscoderTest {

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
   */
  @Test
  public void decodeBinaryValueRecordA() {
    DnsRecord actual = transcoder.decodeBinaryValue(Base64.getDecoder().decode(A_RECORD_BASE64));
    assertNotNull(actual);
    assertEquals(A_RECORD_TYPE, actual.getRecordType());
    assertEquals(A_RECORD_VALUE, actual.getRecordValue());
    assertEquals(A_RECORD_VERSION, actual.getVersion());
    assertEquals(A_RECORD_SERIAL, actual.getSerial());
    assertEquals(A_RECORD_TTL, actual.getTtlSeconds());
    assertEquals(A_RECORD_TIME_STAMP, actual.getTimeStamp());
  }

  /**
   * Decode binary value record cname.
   */
  @Test
  public void decodeBinaryValueRecordCname() {
    DnsRecord actual = transcoder
        .decodeBinaryValue(Base64.getDecoder().decode(CNAME_RECORD_BASE64));
    assertNotNull(actual);
    assertEquals(CNAME_RECORD_TYPE, actual.getRecordType());
    assertEquals(CNAME_RECORD_VALUE, actual.getRecordValue());
    assertEquals(CNAME_RECORD_VERSION, actual.getVersion());
    assertEquals(CNAME_RECORD_SERIAL, actual.getSerial());
    assertEquals(CNAME_RECORD_TTL, actual.getTtlSeconds());
    assertEquals(CNAME_RECORD_TIME_STAMP, actual.getTimeStamp());
  }

  /**
   * Decode binary value record ptr.
   */
  @Test
  public void decodeBinaryValueRecordPtr() {
    DnsRecord actual = transcoder
        .decodeBinaryValue(Base64.getDecoder().decode(PTR_RECORD_BASE64));
    assertNotNull(actual);
    assertEquals(PTR_RECORD_TYPE, actual.getRecordType());
    assertEquals(PTR_RECORD_VALUE, actual.getRecordValue());
    assertEquals(PTR_RECORD_VERSION, actual.getVersion());
    assertEquals(PTR_RECORD_SERIAL, actual.getSerial());
    assertEquals(PTR_RECORD_TTL, actual.getTtlSeconds());
    assertEquals(PTR_RECORD_TIME_STAMP, actual.getTimeStamp());
  }

  /**
   * Decode binary value record unknown.
   */
  @Test
  public void decodeBinaryValueRecordUnknown() {
    DnsRecord actual = transcoder
        .decodeBinaryValue(Base64.getDecoder().decode(UNKNOWN_RECORD_BASE64));
    assertNotNull(actual);
    assertEquals(UNKNOWN_RECORD_TYPE, actual.getRecordType());
    assertEquals(UNKNOWN_RECORD_VALUE, actual.getRecordValue());
    assertEquals(UNKNOWN_RECORD_VERSION, actual.getVersion());
    assertEquals(UNKNOWN_RECORD_SERIAL, actual.getSerial());
    assertEquals(UNKNOWN_RECORD_TTL, actual.getTtlSeconds());
    assertEquals(UNKNOWN_RECORD_TIME_STAMP, actual.getTimeStamp());
  }

  /**
   * Encode binary value.
   */
  @Test
  public void encodeBinaryValue() {
    DnsRecord dnsRecord = new DnsRecord();
    assertNull(transcoder.encodeBinaryValue(dnsRecord));
    dnsRecord.setRecordRawValue(Base64.getDecoder().decode(A_RECORD_BASE64));
    assertArrayEquals(
        Base64.getDecoder().decode(A_RECORD_BASE64),
        transcoder.encodeBinaryValue(dnsRecord));
  }

  /**
   * Gets type.
   */
  @Test
  public void getType() {
    assertEquals(DnsRecord.class, transcoder.getType());
  }
}