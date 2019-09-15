/*
 * Copyright 2019 the original author or authors.
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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.bremersee.dccon.model.DnsRecord;
import org.bremersee.exception.ServiceException;
import org.springframework.util.StringUtils;

/**
 * The dns record data mapper.
 *
 * @author Christian Bremer
 */
@Slf4j
public abstract class DnsRecordDataMapper {

  private DnsRecordDataMapper() {
  }

  /**
   * Parse a dns record.
   *
   * @param data              the data
   * @param dnsRecordSupplier the dns record supplier
   * @return the dns record
   */
  public static DnsRecord parseA(final byte[] data, final Supplier<DnsRecord> dnsRecordSupplier) {
    final DnsRecord dnsRecord = dnsRecordSupplier.get();
    try {
      final InetAddress inetAddress = InetAddress.getByAddress(data);
      dnsRecord.setRecordValue(inetAddress.getHostAddress());
      dnsRecord.setCorrelatedRecordValue(inetAddress.getHostName());

    } catch (UnknownHostException e) {
      if (data.length >= 4) {
        log.warn("msg=[Resolving IP address failed. Trying to parse it.]", e);
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
          String ip = String.valueOf(dis.readUnsignedByte())
              + '.'
              + dis.readUnsignedByte()
              + '.'
              + dis.readUnsignedByte()
              + '.'
              + dis.readUnsignedByte();
          dnsRecord.setRecordValue(ip);
        } catch (IOException ioe) {
          final ServiceException se = ServiceException.internalServerError(
              "Parsing data of A record failed.", ioe);
          log.error("msg=[Parsing data of A record failed.]", se);
          throw se;
        }
      } else {
        final ServiceException se = ServiceException.internalServerError(
            "Parsing data of A record failed.", e);
        log.error("msg=[Parsing data of A record failed.]", se);
        throw se;
      }
    }
    log.debug("msg=[A record parsed.] recordValue=[{}] correlatedRecordValue=[{}]",
        dnsRecord.getRecordValue(), dnsRecord.getCorrelatedRecordValue());
    return dnsRecord;
  }

  /**
   * Parse cname dns record.
   *
   * @param data              the data
   * @param dnsRecordSupplier the dns record supplier
   * @return the dns record
   */
  public static DnsRecord parseCname(
      final byte[] data,
      final Supplier<DnsRecord> dnsRecordSupplier) {
    return parsePtr(data, dnsRecordSupplier);
  }

  /**
   * Parse ptr dns record.
   *
   * @param data              the data
   * @param dnsRecordSupplier the dns record supplier
   * @return the dns record
   */
  public static DnsRecord parsePtr(final byte[] data, final Supplier<DnsRecord> dnsRecordSupplier) {
    final DnsRecord dnsRecord = dnsRecordSupplier.get();
    if (data != null && data.length > 1) {
      try (final DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
        int expectedLength = dis.readUnsignedByte();
        int actualLength = 0;
        final List<String> values = new ArrayList<>();
        StringBuilder valueBuilder = new StringBuilder();
        for (char c : IOUtils.toString(dis, StandardCharsets.UTF_8).toCharArray()) {
          actualLength++;
          if (c > 32) {
            valueBuilder.append(c);
          } else if (valueBuilder.length() > 0) {
            values.add(valueBuilder.toString());
            valueBuilder = new StringBuilder();
          }
        }
        if (valueBuilder.length() > 0) {
          values.add(valueBuilder.toString());
        }
        final String value = StringUtils.collectionToDelimitedString(values, ".");
        log.debug("msg=[PTR record parsed.] ptrValue=[{}] expectedLength=[{}] actualLength=[{}]",
            value, expectedLength, actualLength);

        try {
          final InetAddress inetAddress = InetAddress.getByName(value);
          dnsRecord.setRecordValue(inetAddress.getHostName());
          dnsRecord.setCorrelatedRecordValue(inetAddress.getHostAddress());

        } catch (UnknownHostException e) {
          dnsRecord.setRecordValue(value);
        }

      } catch (IOException e) {
        log.error("Parsing PTR record value failed.", e);
      }
    }
    return dnsRecord;
  }
}
