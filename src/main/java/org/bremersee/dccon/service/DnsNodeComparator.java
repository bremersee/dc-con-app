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
import org.bremersee.dccon.model.DnsNode;

/**
 * The dns node comparator.
 *
 * @author Christian Bremer
 */
public class DnsNodeComparator extends AbstractDnsComparator<DnsNode> {

  /**
   * Instantiates a dns node comparator.
   */
  DnsNodeComparator() {
    this(Boolean.TRUE);
  }

  /**
   * Instantiates a dns node comparator.
   *
   * @param asc use ascending sort order or not
   */
  private DnsNodeComparator(Boolean asc) {
    super(asc);
  }

  @Override
  public int compare(DnsNode o1, DnsNode o2) {
    final String s1 = o1 != null && o1.getName() != null ? o1.getName() : "";
    final String s2 = o2 != null && o2.getName() != null ? o2.getName() : "";

    final String[] sa1 = s1.split(Pattern.quote("."));
    final String[] sa2 = s2.split(Pattern.quote("."));
    if (sa1.length == sa2.length) {
      for (int i = 0; i < sa1.length; i++) {
        int result = format(sa1[i], 3).compareTo(format(sa2[i], 3));
        if (result != 0) {
          return isAsc() ? result : -1 * result;
        }
      }
      return 0;
    }

    int result = format(s1, 3).compareToIgnoreCase(format(s2, 3));
    return isAsc() ? result : -1 * result;
  }
}
