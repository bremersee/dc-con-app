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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.model.DnsRecord;
import org.bremersee.dccon.repository.DnsRecordType;
import org.bremersee.exception.ServiceException;
import org.ldaptive.transcode.AbstractBinaryValueTranscoder;

/**
 * The dns record value transcoder.
 *
 * @author Christian Bremer
 */
@Slf4j
public class DnsRecordValueTranscoder extends AbstractBinaryValueTranscoder<DnsRecord> {

  private static final long ACTIVE_DIRECTORY_START_TIME;

  static {
    final GregorianCalendar cal = new GregorianCalendar();
    cal.setTimeZone(TimeZone.getTimeZone("GMT"));
    cal.set(1601, Calendar.JANUARY, 1, 0, 0, 0);
    cal.set(Calendar.MILLISECOND, 0);
    ACTIVE_DIRECTORY_START_TIME = cal.getTimeInMillis();
  }

  @Override
  public DnsRecord decodeBinaryValue(final byte[] value) {
    if (value == null || value.length < 24) {
      return null;
    }
    try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(value))) {
      int dataLength = Short.reverseBytes((short) dis.readUnsignedShort()); // 2 bytes
      int type = Short.reverseBytes((short) dis.readUnsignedShort());       // 2 bytes
      int version = dis.readUnsignedByte();                                 // 1 byte
      int rank = dis.readUnsignedByte();                                    // 1 byte
      int flags = Short.reverseBytes((short) dis.readUnsignedShort());      // 2 bytes
      int serial = Integer.reverseBytes(dis.readInt());                     // 4 bytes
      int ttlSeconds = dis.readInt();                                       // 4 bytes
      int reserved = Integer.reverseBytes(dis.readInt());                   // 4 bytes
      int timeStamp = Integer.reverseBytes(dis.readInt());                  // 4 bytes
      byte[] data = value.length > 24
          ? Arrays.copyOfRange(value, 24, value.length)
          : new byte[0];

      long timeStampMillis = ACTIVE_DIRECTORY_START_TIME + Duration.ofHours(timeStamp).toMillis();
      OffsetDateTime dateTime = OffsetDateTime.ofInstant(
          new Date(timeStampMillis).toInstant(),
          ZoneOffset.UTC);
      DnsRecordType typeMapping = DnsRecordType.fromValue(type);

      log.debug("msg=[DNS record value decoded.] dataLength=[{}] type=[{}] version=[{}] "
              + "rank=[{}] flags=[{}] serial=[{}] ttlSeconds=[{}] reserved=[{}] timeStamp=[{}] "
              + "data(size)=[{}]", dataLength, typeMapping.name(), version, rank, flags, serial,
          ttlSeconds, reserved, dateTime, data.length);

      final DnsRecord dnsRecord = new DnsRecord();
      dnsRecord.setRecordRawValue(value);
      dnsRecord.setRecordType(typeMapping.name());
      dnsRecord.setSerial(serial);
      dnsRecord.setTimeStamp(dateTime);
      dnsRecord.setTtlSeconds(ttlSeconds);
      dnsRecord.setVersion(version);

      return typeMapping.mapData(data, () -> dnsRecord);

    } catch (final IOException e) {

      final ServiceException se = ServiceException.internalServerError(
          "Decoding dns record value failed.",
          "org.bremersee:dc-con-app:d62e15c8-5410-4608-b663-20a716908102", e);
      log.error("msg=[Decoding dns record value failed.]", se);
      throw se;
    }
  }

  @Override
  public byte[] encodeBinaryValue(final DnsRecord value) {
    return value.getRecordRawValue();
  }

  @Override
  public Class<DnsRecord> getType() {
    return DnsRecord.class;
  }

}
