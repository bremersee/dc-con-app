/*
 * Copyright 2019-2020 the original author or authors.
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

import java.util.Optional;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;
import org.bremersee.dccon.model.DnsZone;
import org.springframework.validation.annotation.Validated;

/**
 * The dns zone repository.
 *
 * @author Christian Bremer
 */
@Validated
public interface DnsZoneRepository {

  /**
   * Determines whether the given zone is a reverse zone.
   *
   * @param dnsZoneName the dns zone name
   * @return the boolean
   */
  boolean isDnsReverseZone(final String dnsZoneName);

  /**
   * Find all dns zones.
   *
   * @return the dns zones
   */
  Stream<DnsZone> findAll();

  /**
   * Find dns reverse zones stream.
   *
   * @return the stream
   */
  default Stream<DnsZone> findDnsReverseZones() {
    return findAll().filter(dnsZone -> isDnsReverseZone(dnsZone.getName()));
  }

  /**
   * Find non dns reverse zones stream.
   *
   * @return the stream
   */
  default Stream<DnsZone> findNonDnsReverseZones() {
    return findAll().filter(dnsZone -> !isDnsReverseZone(dnsZone.getName()));
  }

  /**
   * Check whether dns zone exists or not.
   *
   * @param zoneName the zone name
   * @return {@code true} if the dns zone exists, otherwise {@code false}
   */
  boolean exists(@NotNull String zoneName);

  /**
   * Find dns zone.
   *
   * @param zoneName the zone name
   * @return the dns zone
   */
  Optional<DnsZone> findOne(@NotNull String zoneName);

  /**
   * Save dns zone.
   *
   * @param zoneName the zone name
   * @return the dns zone
   */
  DnsZone save(@NotNull String zoneName);

  /**
   * Delete dns zone.
   *
   * @param zoneName the zone name
   * @return {@code true} is the repository was deleted, otherwise {@code false}
   */
  boolean delete(@NotNull String zoneName);


}
