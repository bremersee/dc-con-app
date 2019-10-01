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

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.bremersee.dccon.model.DhcpLease;
import org.bremersee.dccon.model.DnsNode;
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.model.UnknownFilter;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;

/**
 * The name server service.
 *
 * @author Christian Bremer
 */
@Validated
public interface NameServerService {

  /**
   * Query dns nodes.
   *
   * @param query         the query, can be a host name, an IP or a MAC address
   * @param unknownFilter the unknown filter (default is {@link UnknownFilter#NO_UNKNOWN}
   * @return found dns nodes
   */
  List<DnsNode> query(@NotNull String query, @Nullable UnknownFilter unknownFilter);

  /**
   * Get dhcp leases.
   *
   * @param all  if {@code true}, expired leases will also be returned, otherwise only active ones
   *             (default is {@code false})
   * @param sort the sort order (default is {@link DhcpLease#SORT_ORDER_BEGIN_HOSTNAME})
   * @return the dhcp leases
   */
  List<DhcpLease> getDhcpLeases(@Nullable Boolean all, @Nullable String sort);

  /**
   * Get dns zones.
   *
   * @return the dns zones
   */
  List<DnsZone> getDnsZones();

  /**
   * Add dns zone.
   *
   * @param dnsZone the dns zone
   * @return the added dns zone
   */
  DnsZone addDnsZone(@NotNull @Valid DnsZone dnsZone);

  /**
   * Delete dns zone.
   *
   * @param zoneName the zone name
   * @return {@code true} if the dns zone was removed; {@code false} if the dns zone didn't exist
   */
  Boolean deleteDnsZone(@NotNull String zoneName);


  /**
   * Gets dns nodes.
   *
   * @param zoneName      the zone name
   * @param unknownFilter the unknown filter (default is {@link UnknownFilter#NO_UNKNOWN}
   * @return the dns nodes
   */
  List<DnsNode> getDnsNodes(@NotNull String zoneName, @Nullable UnknownFilter unknownFilter);

  /**
   * Save dns node.
   *
   * @param zoneName the zone name
   * @param dnsNode  the dns node
   * @return the dns node
   */
  Optional<DnsNode> save(
      @NotNull String zoneName,
      @NotNull @Valid DnsNode dnsNode);

  /**
   * Get dns node.
   *
   * @param zoneName      the zone name
   * @param nodeName      the node name
   * @param unknownFilter the unknown filter (default is {@link UnknownFilter#NO_UNKNOWN}
   * @return the dns node
   */
  Optional<DnsNode> getDnsNode(
      @NotNull String zoneName,
      @NotNull String nodeName,
      @Nullable UnknownFilter unknownFilter);

  /**
   * Delete dns node.
   *
   * @param zoneName the zone name
   * @param nodeName the node name
   * @return {@code true} if the dns node was removed; {@code false} if dns node didn't exist
   */
  Boolean deleteDnsNode(
      @NotNull String zoneName,
      @NotNull String nodeName);

  /**
   * Delete all dns nodes.
   *
   * @param zoneName the zone name
   */
  @SuppressWarnings("unused")
  void deleteAllDnsNodes(
      @NotNull String zoneName);

  /**
   * Delete all dns nodes.
   *
   * @param zoneName  the zone name
   * @param nodeNames the node names
   */
  void deleteAllDnsNodes(
      @NotNull String zoneName,
      @Nullable List<String> nodeNames);

}
