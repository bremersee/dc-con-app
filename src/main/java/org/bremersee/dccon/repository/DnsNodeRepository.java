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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;
import org.bremersee.dccon.model.DnsNode;
import org.bremersee.dccon.model.DnsPair;
import org.bremersee.dccon.model.DnsRecord;
import org.bremersee.dccon.model.UnknownFilter;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;

/**
 * The dns node repository.
 *
 * @author Christian Bremer
 */
@Validated
public interface DnsNodeRepository {

  /**
   * Get unknown filter or default one.
   *
   * @param unknownFilter the unknown filter
   * @return the unknown filter
   */
  @NotNull
  default UnknownFilter unknownFilter(@Nullable UnknownFilter unknownFilter) {
    return unknownFilter != null ? unknownFilter : UnknownFilter.NO_UNKNOWN;
  }

  /**
   * Find dns nodes by ips.
   *
   * @param ips the ips
   * @param unknownFilter the unknown filter
   * @return the dns nodes
   */
  List<DnsNode> findByIps(@NotNull Set<String> ips, UnknownFilter unknownFilter);

  /**
   * Find dns node by host name.
   *
   * @param hostName the host name, can be a simple host name or a full qualified domain name
   * @param unknownFilter the unknown filter
   * @return the dns node
   */
  Optional<DnsNode> findByHostName(@NotNull String hostName, UnknownFilter unknownFilter);

  /**
   * Find all.
   *
   * @param zoneName the zone name
   * @param unknownFilter the unknown filter (default is {@link UnknownFilter#NO_UNKNOWN}
   * @param query the query
   * @return the dns nodes
   */
  Stream<DnsNode> findAll(
      @NotNull String zoneName,
      @Nullable UnknownFilter unknownFilter,
      @Nullable String query);

  /**
   * Check whether dns node exists or not.
   *
   * @param zoneName the zone name
   * @param nodeName the node name
   * @param unknownFilter the unknown filter (default is {@link UnknownFilter#NO_UNKNOWN}
   * @return {@code true} if the dns node exists, otherwise {@code false}
   */
  boolean exists(
      @NotNull String zoneName,
      @NotNull String nodeName,
      @Nullable UnknownFilter unknownFilter);

  /**
   * Find dns node by zone name and node name.
   *
   * @param zoneName the zone name
   * @param nodeName the node name
   * @param unknownFilter the unknown filter (default is {@link UnknownFilter#NO_UNKNOWN}
   * @return the dns node
   */
  Optional<DnsNode> findOne(
      @NotNull String zoneName,
      @NotNull String nodeName,
      @Nullable UnknownFilter unknownFilter);

  /**
   * Find correlated dns node optional.
   *
   * @param zoneName the zone name
   * @param record the record
   * @return the optional
   */
  Optional<DnsPair> findCorrelatedDnsNode(
      @NotNull String zoneName,
      @NotNull DnsRecord record);

  /**
   * Save dns node.
   *
   * @param zoneName the zone name
   * @param dnsNode the dns node
   * @return the dns node (will be {@link Optional#empty()}, if the node has no records)
   */
  Optional<DnsNode> save(@NotNull String zoneName, @NotNull DnsNode dnsNode);

  /**
   * Delete a dns node.
   *
   * @param zoneName the zone name
   * @param nodeName the node name
   * @return {@code true} if the dns node was removed; {@code false} if dns node didn't exist
   */
  default boolean delete(@NotNull String zoneName, @NotNull String nodeName) {
    return findOne(zoneName, nodeName, UnknownFilter.ALL)
        .map(node -> delete(zoneName, node))
        .orElse(false);
  }

  /**
   * Delete dns node.
   *
   * @param zoneName the zone name
   * @param node the node
   * @return the boolean
   */
  boolean delete(@NotNull String zoneName, @NotNull DnsNode node);

  /**
   * Delete all dns nodes of the specified dns zone.
   *
   * @param zoneName the zone name
   */
  default void deleteAll(@NotNull String zoneName) {
    findAll(zoneName, UnknownFilter.ALL, null)
        .forEach(dnsNode -> delete(zoneName, dnsNode));
  }

  /**
   * Delete all dns nodes with the specified names from the specified dns zone.
   *
   * @param zoneName the zone name
   * @param nodeNames the node names
   */
  default void deleteAll(@NotNull String zoneName, @Nullable Collection<String> nodeNames) {
    if (nodeNames != null && !nodeNames.isEmpty()) {
      for (String nodeName : new LinkedHashSet<>(nodeNames)) {
        findOne(zoneName, nodeName, UnknownFilter.ALL)
            .ifPresent(dnsNode -> delete(zoneName, dnsNode));
      }
    }
  }

}
