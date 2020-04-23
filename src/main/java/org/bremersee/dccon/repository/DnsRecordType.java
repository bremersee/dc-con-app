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
 * The enum Dns record type.
 *
 * @author Christian Bremer
 */
public enum DnsRecordType {

  /**
   * Unknown dns record type.
   */
  UNKNOWN(-1, null),

  /**
   * A dns record type.
   */
  A(1, DnsRecordDataMapper::parseA),

  /**
   * Ns dns record type.
   */
  NS(2, null),

  /**
   * Md dns record type.
   */
  MD(3, null),

  /**
   * Mf dns record type.
   */
  MF(4, null),

  /**
   * Cname dns record type.
   */
  CNAME(5, DnsRecordDataMapper::parseCname),

  /**
   * Soa dns record type.
   */
  SOA(6, null),

  /**
   * Mb dns record type.
   */
  MB(7, null),

  /**
   * Mg dns record type.
   */
  MG(8, null),

  /**
   * Mr dns record type.
   */
  MR(9, null),

  /**
   * Null dns record type.
   */
  NULL(10, null),

  /**
   * Wks dns record type.
   */
  WKS(11, null),

  /**
   * Ptr dns record type.
   */
  PTR(12, DnsRecordDataMapper::parsePtr),

  /**
   * Hinfo dns record type.
   */
  HINFO(13, null),

  /**
   * Minfo dns record type.
   */
  MINFO(14, null),

  /**
   * Mx dns record type.
   */
  MX(15, null),

  /**
   * Txt dns record type.
   */
  TXT(16, null),

  /**
   * Rp dns record type.
   */
  RP(17, null),

  /**
   * Afsdb dns record type.
   */
  AFSDB(18, null),

  /**
   * X 25 dns record type.
   */
  X25(19, null),

  /**
   * Isdn dns record type.
   */
  ISDN(20, null),

  /**
   * Rt dns record type.
   */
  RT(21, null),

  /**
   * Nsap dns record type.
   */
  NSAP(22, null),

  /**
   * Nsap ptr dns record type.
   */
  NSAP_PTR(23, null),

  /**
   * Sig dns record type.
   */
  SIG(24, null),

  /**
   * Key dns record type.
   */
  KEY(25, null),

  /**
   * Px dns record type.
   */
  PX(26, null),

  /**
   * Gpos dns record type.
   */
  GPOS(27, null),

  /**
   * Aaaa dns record type.
   */
  AAAA(28, null),

  /**
   * Loc dns record type.
   */
  LOC(29, null),

  /**
   * Nxt dns record type.
   */
  NXT(30, null),

  /**
   * Eid dns record type.
   */
  EID(31, null),

  /**
   * Nimloc dns record type.
   */
  NIMLOC(32, null),

  /**
   * Srv dns record type.
   */
  SRV(33, null),

  /**
   * Atma dns record type.
   */
  ATMA(34, null),

  /**
   * Naptr dns record type.
   */
  NAPTR(35, null),

  /**
   * Kx dns record type.
   */
  KX(36, null),

  /**
   * Cert dns record type.
   */
  CERT(37, null),

  /**
   * A 6 dns record type.
   */
  A6(38, null),

  /**
   * Dname dns record type.
   */
  DNAME(39, null),

  /**
   * Sink dns record type.
   */
  SINK(40, null),

  /**
   * Opt dns record type.
   */
  OPT(41, null),

  /**
   * Apl dns record type.
   */
  APL(42, null),
  /**
   * Ds dns record type.
   */
  DS(43, null),

  /**
   * Sshfp dns record type.
   */
  SSHFP(44, null),

  /**
   * Ipseckey dns record type.
   */
  IPSECKEY(45, null),

  /**
   * Rrsig dns record type.
   */
  RRSIG(46, null),

  /**
   * Nsec dns record type.
   */
  NSEC(47, null),

  /**
   * Dnskey dns record type.
   */
  DNSKEY(48, null),

  /**
   * Dhcid dns record type.
   */
  DHCID(49, null),

  /**
   * Nsec 3 dns record type.
   */
  NSEC3(50, null),

  /**
   * Nsec 3 param dns record type.
   */
  NSEC3PARAM(51, null),

  /**
   * Tlsa dns record type.
   */
  TLSA(52, null),

  /**
   * Hip dns record type.
   */
  HIP(55, null),

  /**
   * Ninfo dns record type.
   */
  NINFO(56, null),

  /**
   * Rkey dns record type.
   */
  RKEY(57, null),

  /**
   * Talink dns record type.
   */
  TALINK(58, null),

  /**
   * Cds dns record type.
   */
  CDS(59, null),

  /**
   * Cdnskey dns record type.
   */
  CDNSKEY(60, null),

  /**
   * Openpgpkey dns record type.
   */
  OPENPGPKEY(61, null),

  /**
   * Csync dns record type.
   */
  CSYNC(62, null),

  /**
   * Spf dns record type.
   */
  SPF(99, null),

  /**
   * Uinfo dns record type.
   */
  UINFO(100, null),

  /**
   * Uid dns record type.
   */
  UID(101, null),

  /**
   * Gid dns record type.
   */
  GID(102, null),

  /**
   * Unspec dns record type.
   */
  UNSPEC(103, null),

  /**
   * Nid dns record type.
   */
  NID(104, null),

  /**
   * L 32 dns record type.
   */
  L32(105, null),

  /**
   * L 64 dns record type.
   */
  L64(106, null),

  /**
   * Lp dns record type.
   */
  LP(107, null),

  /**
   * Eui 48 dns record type.
   */
  EUI48(108, null),

  /**
   * Eui 64 dns record type.
   */
  EUI64(109, null),

  /**
   * Tkey dns record type.
   */
  TKEY(249, null),

  /**
   * Tsig dns record type.
   */
  TSIG(250, null),

  /**
   * Ixfr dns record type.
   */
  IXFR(251, null),

  /**
   * Axfr dns record type.
   */
  AXFR(252, null),

  /**
   * Mailb dns record type.
   */
  MAILB(253, null),

  /**
   * Maila dns record type.
   */
  MAILA(254, null),

  /**
   * Any dns record type.
   */
  ANY(255, null),

  /**
   * Uri dns record type.
   */
  URI(256, null),

  /**
   * Caa dns record type.
   */
  CAA(257, null),

  /**
   * Ta dns record type.
   */
  TA(32768, null),

  /**
   * Dlv dns record type.
   */
  DLV(32769, null);

  /**
   * Internal lookup table to map values to types.
   */
  private final static Map<Integer, DnsRecordType> VALUE_TYPE_MAP = new HashMap<>();

  /**
   * Internal lookup table to map strings to types.
   */
  private final static Map<String, DnsRecordType> STRING_TYPE_MAP = new HashMap<>();

  static {
    for (DnsRecordType t : DnsRecordType.values()) {
      VALUE_TYPE_MAP.put(t.getValue(), t);
      STRING_TYPE_MAP.put(t.name(), t);
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

  /**
   * Determine whether dns record one is correlated the dns record two.
   *
   * @param dnsRecord1 the dns record 1
   * @param dnsRecord2 the dns record 2
   * @return the boolean
   */
  public static boolean areCorrelated(final DnsRecord dnsRecord1, final DnsRecord dnsRecord2) {
    return dnsRecord1 != null && dnsRecord2 != null
        && areCorrelated(dnsRecord1.getRecordType(), dnsRecord2.getRecordType());
  }

  /**
   * Determine whether dns record one is correlated the dns record two.
   *
   * @param dnsRecordType1 the dns record type 1
   * @param dnsRecordType2 the dns record type 2
   * @return the boolean
   */
  public static boolean areCorrelated(final String dnsRecordType1, final String dnsRecordType2) {
    return DnsRecordType.fromValue(dnsRecordType1)
        .isCorrelatedWith(DnsRecordType.fromValue(dnsRecordType2));
  }

  /**
   * Find dns record type by integer.
   *
   * @param value the value
   * @return the dns record type
   */
  public static DnsRecordType fromValue(final int value) {
    return VALUE_TYPE_MAP.getOrDefault(value, UNKNOWN);
  }

  /**
   * Find dns record type by string.
   *
   * @param value the value
   * @return the dns record type
   */
  public static DnsRecordType fromValue(final String value) {
    if (value == null) {
      return UNKNOWN;
    }
    return STRING_TYPE_MAP.getOrDefault(value.toUpperCase(), UNKNOWN);
  }

  /**
   * Determine whether this dns record equals the given one.
   *
   * @param recordType the record type
   * @return the boolean
   */
  public boolean is(final String recordType) {
    return this == fromValue(recordType);
  }

  /**
   * Determine whether this dns record is correlated the dns record two.
   *
   * @param dnsRecordType the dns record type
   * @return the boolean
   */
  public boolean isCorrelatedWith(DnsRecordType dnsRecordType) {
    switch (this) {
      case A:
      case AAAA:
        return PTR == dnsRecordType;
      case PTR:
        return A == dnsRecordType || AAAA == dnsRecordType;
      default:
        return false;
    }
  }

  /**
   * Map data from active directory to dns record.
   *
   * @param data the data
   * @param dnsRecordSupplier the dns record supplier
   * @return the dns record
   */
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

}
