/*
 * Copyright 2025 the original author or authors.
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

package org.bremersee.dccon.controller.ui.model;

import static java.util.Objects.nonNull;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.Optional;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bremersee.dccon.model.DnsEntry;
import org.bremersee.dccon.model.DnsEntryType;
import org.bremersee.dccon.service.DnsService;

/**
 * The type DnsEntryAddRequest.
 *
 * @author Christian Bremer
 */
@Data
@NoArgsConstructor
public class DnsEntryAddRequest {

  private String name;

  private DnsEntryType type = DnsEntryType.A;

  private String value;

  private boolean addReverseEntry;

  private String reverseZoneName;

  private String nameOfReverseEntry;

  private String valueOfReverseEntry;

  public DnsEntryAddRequest(String zoneName) {
    if (nonNull(zoneName) && zoneName.toLowerCase().endsWith(DnsService.REVERSE_ZONE_POSTFIX)) {
      type = DnsEntryType.PTR;
    }
  }

  public DnsEntry toDnsEntry(String zoneName) {
    DnsEntry dnsEntry = new DnsEntry();
    dnsEntry.setZoneName(zoneName);
    dnsEntry.setName(name);
    dnsEntry.setType(type);
    dnsEntry.setValue(value);
    return dnsEntry;
  }

  public Optional<DnsEntry> toReverseDnsEntry() {
    if (!addReverseEntry || isEmpty(reverseZoneName)
        || isEmpty(nameOfReverseEntry) || isEmpty(valueOfReverseEntry)
        || !(DnsEntryType.A.equals(type) || DnsEntryType.AAAA.equals(type))) {
      return Optional.empty();
    }
    DnsEntry dnsEntry = new DnsEntry();
    dnsEntry.setZoneName(reverseZoneName);
    dnsEntry.setName(nameOfReverseEntry);
    dnsEntry.setType(DnsEntryType.PTR);
    dnsEntry.setValue(valueOfReverseEntry);
    return Optional.of(dnsEntry);
  }
}
