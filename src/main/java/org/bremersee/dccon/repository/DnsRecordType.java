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

package org.bremersee.dccon.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.Getter;
import org.bremersee.dccon.model.DnsRecord;
import org.bremersee.dccon.repository.ldap.transcoder.DnsRecordDataMapper;
import org.ldaptive.io.Hex;

/**
 * @author Christian Bremer
 */
public enum DnsRecordType {

  UNKNOWN(-1, null),
  A(1, DnsRecordDataMapper::parseA),
  NS(2, null),
  MD(3, null),
  MF(4, null),
  CNAME(5, DnsRecordDataMapper::parseCname),
  SOA(6, null),
  MB(7, null),
  MG(8, null),
  MR(9, null),
  NULL(10, null),
  WKS(11, null),
  PTR(12, DnsRecordDataMapper::parsePtr),
  HINFO(13, null),
  MINFO(14, null),
  MX(15, null),
  TXT(16, null),
  RP(17, null),
  AFSDB(18, null),
  X25(19, null),
  ISDN(20, null),
  RT(21, null),
  NSAP(22, null),
  NSAP_PTR(23, null),
  SIG(24, null),
  KEY(25, null),
  PX(26, null),
  GPOS(27, null),
  AAAA(28, null),
  LOC(29, null),
  NXT(30, null),
  EID(31, null),
  NIMLOC(32, null),
  SRV(33, null),
  ATMA(34, null),
  NAPTR(35, null),
  KX(36, null),
  CERT(37, null),
  A6(38, null),
  DNAME(39, null),
  SINK(40, null),
  OPT(41, null),
  APL(42, null),
  DS(43, null),
  SSHFP(44, null),
  IPSECKEY(45, null),
  RRSIG(46, null),
  NSEC(47, null),
  DNSKEY(48, null),
  DHCID(49, null),
  NSEC3(50, null),
  NSEC3PARAM(51, null),
  TLSA(52, null),
  HIP(55, null),
  NINFO(56, null),
  RKEY(57, null),
  TALINK(58, null),
  CDS(59, null),
  CDNSKEY(60, null),
  OPENPGPKEY(61, null),
  CSYNC(62, null),
  SPF(99, null),
  UINFO(100, null),
  UID(101, null),
  GID(102, null),
  UNSPEC(103, null),
  NID(104, null),
  L32(105, null),
  L64(106, null),
  LP(107, null),
  EUI48(108, null),
  EUI64(109, null),
  TKEY(249, null),
  TSIG(250, null),
  IXFR(251, null),
  AXFR(252, null),
  MAILB(253, null),
  MAILA(254, null),
  ANY(255, null),
  URI(256, null),
  CAA(257, null),
  TA(32768, null),
  DLV(32769, null);

  /**
   * Internal lookup table to map values to types.
   */
  private final static Map<Integer, DnsRecordType> VALUE_TYPE_MAP = new HashMap<>();

  static {
    for (DnsRecordType t : DnsRecordType.values()) {
      VALUE_TYPE_MAP.put(t.getValue(), t);
    }
  }

  /**
   * The value of this dns record type.
   */
  @Getter(AccessLevel.PACKAGE)
  private final int value;

  private final BiFunction<byte[], Supplier<DnsRecord>, DnsRecord> dataMapper;

  DnsRecordType(
      final int value,
      final BiFunction<byte[], Supplier<DnsRecord>, DnsRecord> dataMapper) {
    this.value = value;
    this.dataMapper = dataMapper;
  }

  public DnsRecord mapData(final byte[] data, final Supplier<DnsRecord> dnsRecordSupplier) {
    final DnsRecord dnsRecord = dnsRecordSupplier.get();
    if (dataMapper == null) {
      if (data == null || data.length == 0) {
        dnsRecord.setRecordValue("");
      } else {
        dnsRecord.setRecordValue(new String(Hex.encode(data)));
      }
      return dnsRecord;
    }
    return dataMapper.apply(data, () -> dnsRecord);
  }

  public static DnsRecordType fromValue(final int value) {
    return VALUE_TYPE_MAP.getOrDefault(value, DnsRecordType.UNKNOWN);
  }

  public static DnsRecordType fromValue(final String value) {
    try {
      return DnsRecordType.valueOf(value);
    } catch (Exception e) {
      return UNKNOWN;
    }
  }

}
