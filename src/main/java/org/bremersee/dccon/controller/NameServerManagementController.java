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

package org.bremersee.dccon.controller;

import java.util.List;
import javax.validation.Valid;
import org.bremersee.dccon.api.NameServerManagementApi;
import org.bremersee.dccon.model.DhcpLease;
import org.bremersee.dccon.model.DnsNode;
import org.bremersee.dccon.model.DnsZone;
import org.bremersee.dccon.model.UnknownFilter;
import org.bremersee.dccon.service.NameServerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * The name server management controller.
 *
 * @author Christian Bremer
 */
@RestController
public class NameServerManagementController implements NameServerManagementApi {

  private NameServerService nameServerService;

  /**
   * Instantiates a new name server management controller.
   *
   * @param nameServerService the name server service
   */
  public NameServerManagementController(
      final NameServerService nameServerService) {
    this.nameServerService = nameServerService;
  }

  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LOCAL_USER')")
  @Override
  public ResponseEntity<List<DhcpLease>> getDhcpLeases(
      final Boolean all,
      final String sort) {
    return ResponseEntity.ok(nameServerService.getDhcpLeases(all, sort));
  }

  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LOCAL_USER')")
  @Override
  public ResponseEntity<List<DnsZone>> getDnsZones() {
    return ResponseEntity.ok(nameServerService.getDnsZones());
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<DnsZone> addDnsZone(
      @Valid final DnsZone dnsZone) {
    return ResponseEntity.ok(nameServerService.addDnsZone(dnsZone));
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<Boolean> deleteDnsZone(
      final String zoneName) {
    return ResponseEntity.ok(nameServerService.deleteDnsZone(zoneName));
  }

  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LOCAL_USER')")
  @Override
  public ResponseEntity<List<DnsNode>> getDnsNodes(
      final String zoneName,
      final UnknownFilter unknownFilter) {
    return ResponseEntity.ok(nameServerService.getDnsNodes(zoneName, unknownFilter));
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<DnsNode> saveDnsNode(
      String zoneName,
      @Valid DnsNode dnsNode) {
    return nameServerService.save(zoneName, dnsNode)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
  }

  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LOCAL_USER')")
  @Override
  public ResponseEntity<DnsNode> getDnsNode(
      String zoneName,
      String nodeName,
      UnknownFilter unknownFilter) {
    return ResponseEntity.of(nameServerService.getDnsNode(zoneName, nodeName, unknownFilter));
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<Boolean> deleteDnsNode(String zoneName, String nodeName) {
    return ResponseEntity.ok(nameServerService.deleteDnsNode(zoneName, nodeName));
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Override
  public ResponseEntity<Void> deleteAllDnsNodes(String zoneName, List<String> nodeNames) {
    nameServerService.deleteAllDnsNodes(zoneName, nodeNames);
    return ResponseEntity.ok().build();
  }

}
