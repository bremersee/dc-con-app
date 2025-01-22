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
 * The type DnsEntryEditRequest.
 *
 * @author Christian Bremer
 */
@Data
@NoArgsConstructor
public class DnsEntryEditRequest {

  private String newName;

  private DnsEntryType newType;

  private String newValue;

  private boolean updateReverseEntry;

  private String newNameOfReverseEntry;

  private String newValueOfReverseEntry;

  public DnsEntryEditRequest(DnsEntry dnsEntry) {
    this(dnsEntry, null);
  }

  public DnsEntryEditRequest(DnsEntry dnsEntry, DnsEntry reverseDnsEntry) {
    this.newName = dnsEntry.getName();
    this.newType = dnsEntry.getType();
    this.newValue = dnsEntry.getValue();
    if (nonNull(reverseDnsEntry)) {
      this.updateReverseEntry = true;
      this.newNameOfReverseEntry = reverseDnsEntry.getName();
      this.newValueOfReverseEntry = reverseDnsEntry.getValue();
    }
  }

  public DnsEntry toNewDnsEntry() {
    DnsEntry dnsEntry = new DnsEntry();
    dnsEntry.setName(newName);
    dnsEntry.setType(newType);
    dnsEntry.setValue(newValue);
    return dnsEntry;
  }

}
