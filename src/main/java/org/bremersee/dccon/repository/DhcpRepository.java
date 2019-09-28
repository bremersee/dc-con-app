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

package org.bremersee.dccon.repository;

import java.util.List;
import java.util.Map;
import org.bremersee.dccon.model.DhcpLease;
import org.springframework.validation.annotation.Validated;

/**
 * The dhcp repository.
 *
 * @author Christian Bremer
 */
@Validated
public interface DhcpRepository {

  /**
   * Find all dhcp leases.
   *
   * @return the dhcp leases
   */
  List<DhcpLease> findAll();

  /**
   * Find active by ip.
   *
   * @return the dhcp lease map with ip as key
   */
  Map<String, DhcpLease> findActiveByIp();

  /**
   * Find active by host name.
   *
   * @return the dhcp lease map with host name as key
   */
  Map<String, DhcpLease> findActiveByHostName();

}
