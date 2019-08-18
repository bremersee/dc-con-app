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

package org.bremersee.dccon.business;

import java.util.List;
import org.bremersee.dccon.model.DhcpLease;
import org.springframework.lang.Nullable;

/**
 * The dhcp lease list.
 *
 * @author Christian Bremer
 */
public interface DhcpLeaseList {

  /**
   * Gets dhcp leases.
   *
   * @param all if {@code true}, expired leases will also be returned, otherwise only active ones
   * @return the dhcp leases
   */
  List<DhcpLease> getDhcpLeases(@Nullable Boolean all);

}
