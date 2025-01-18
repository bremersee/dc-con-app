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

package org.bremersee.dccon.model;

import static java.util.Objects.isNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

/**
 * The type DnsZoneEntries.
 *
 * @author Christian Bremer
 */
@ToString
@EqualsAndHashCode
public class DnsZoneEntries<T> {

  @Setter
  private List<DnsEntry> dnsEntries;

  @Setter
  private T dnsZoneEntries;

  @Hidden
  @JsonIgnore
  private final Supplier<T> dnsEntriesSupplier;

  public DnsZoneEntries(Supplier<T> dnsEntriesSupplier) {
    this.dnsEntriesSupplier = dnsEntriesSupplier;
  }

  public List<DnsEntry> getDnsEntries() {
    if (isNull(dnsEntries)) {
      dnsEntries = new ArrayList<>();
    }
    return dnsEntries;
  }

  public T getDnsZoneEntries() {
    if (isNull(dnsZoneEntries)) {
      dnsZoneEntries = dnsEntriesSupplier.get();
    }
    return dnsZoneEntries;
  }

}
