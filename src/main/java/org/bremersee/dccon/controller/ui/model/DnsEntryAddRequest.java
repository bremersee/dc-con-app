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

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bremersee.dccon.model.DnsEntry;
import org.bremersee.dccon.model.DnsEntryType;

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

  public DnsEntryAddRequest(String zoneName) {
    if (nonNull(zoneName) && zoneName.toLowerCase().endsWith(".in-addr.arpa")) {
      type = DnsEntryType.PTR;
    }
  }

  public DnsEntry toDnsEntry() {
    DnsEntry dnsEntry = new DnsEntry();
    dnsEntry.setName(name);
    dnsEntry.setType(type);
    dnsEntry.setValue(value);
    return dnsEntry;
  }
}
