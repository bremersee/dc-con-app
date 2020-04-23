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

package org.bremersee.dccon.service;

import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.repository.DnsZoneRepository;

/**
 * The dns zone comparator.
 *
 * @author Christian Bremer
 */
@Slf4j
public class DnsZoneComparator extends AbstractDnsComparator<DnsZone> {

  private DnsZoneRepository dnsZoneRepository;

  /**
   * Instantiates a new dns zone comparator.
   *
   * @param dnsZoneRepository the dns zone repository
   */
  DnsZoneComparator(DnsZoneRepository dnsZoneRepository) {
    super(true);
    this.dnsZoneRepository = dnsZoneRepository;
  }

  @Override
  public int compare(DnsZone o1, DnsZone o2) {
    if (o1 != null && Boolean.TRUE.equals(o1.getDefaultZone())) {
      return -1;
    } else if (o2 != null && Boolean.TRUE.equals(o2.getDefaultZone())) {
      return 1;
    }
    final String s1 = o1 != null && o1.getName() != null ? o1.getName() : "";
    final String s2 = o2 != null && o2.getName() != null ? o2.getName() : "";
    log.debug("msg=[Comparing zones.] zone1=[{}] zone2=[{}]", s1, s2);
    if (dnsZoneRepository.isDnsReverseZone(s1) && dnsZoneRepository.isDnsReverseZone(s2)) {
      final String[] sa1 = s1.split(Pattern.quote("."));
      final String[] sa2 = s2.split(Pattern.quote("."));
      int c = sa2.length - sa1.length;
      if (c == 0) {
        c = compare(sa1, sa2);
      }
      log.debug("msg=[Both zones are dns reverse zones.] result=[{}]", c);
      return isAsc() ? c : -1 * c;
    } else if (!dnsZoneRepository.isDnsReverseZone(s1) && dnsZoneRepository.isDnsReverseZone(s2)) {
      log.debug("msg=[First is non reverse zone, second is reverse zone.] "
          + "first=[{}] second=[{}] result=[-1]", s1, s2);
      return isAsc() ? -1 : 1;
    } else if (dnsZoneRepository.isDnsReverseZone(s1) && !dnsZoneRepository.isDnsReverseZone(s2)) {
      log.debug("msg=[First is reverse zone, second is non reverse zone.] "
          + "first=[{}] second=[{}] result=[1]", s1, s2);
      return isAsc() ? 1 : -1;
    }
    final int result = s1.compareToIgnoreCase(s2);
    log.debug("msg=[Both zones are non dns reverse zones.] result=[{}]", result);
    return isAsc() ? result : -1 * result;
  }

  private int compare(String[] sa1, String[] sa2) {
    final String[] sav1 = sa1 != null ? sa1 : new String[0];
    final String[] sav2 = sa2 != null ? sa2 : new String[0];
    final int len = Math.min(sav1.length, sav2.length);
    for (int i = len - 1; i >= 0; i--) {
      int c = compare(sav1[i], sav2[i]);
      if (c != 0) {
        return c;
      }
    }
    return 0;
  }

  private int compare(String s1, String s2) {
    return format(s1, 3).compareToIgnoreCase(format(s2, 3));
  }

}
