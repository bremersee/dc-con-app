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

package org.bremersee.dccon.repository;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.bremersee.dccon.model.DnsEntry;
import org.bremersee.dccon.model.DnsEntryType;
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.model.DnsZoneType;
import org.ldaptive.dn.Dn;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;

/**
 * The interface DnsRepository.
 *
 * @author Christian Bremer
 */
@Validated
public interface DnsRepository {

  List<String> findDnsZoneNames(@Nullable DnsZoneType type);

  Optional<DnsZone> findDnsZone(@NotEmpty String zoneName);

  void createDnsZone(@NotEmpty String zoneName);

  void deleteDnsZone(@NotEmpty String zoneName);


  Stream<DnsEntry> findDnsEntries(
      @NotEmpty String zoneName,
      @NotEmpty String name,
      @NotNull DnsEntryType type);

  Optional<DnsEntry> findDnsEntryWithConflict(Dn zoneDn, DnsEntry dnsEntry);

  DnsEntry setCommonAttributes(@NotNull Dn zoneDn, @NotNull DnsEntry entry);

  void addDnsEntry(@NotNull DnsEntry entry);

  void updateDnsEntry(@NotNull DnsEntry entry, @NotEmpty String newValue);

  void deleteDnsEntry(@NotNull DnsEntry entry);

}
