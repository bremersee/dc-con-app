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

import org.bremersee.ldaptive.LdaptiveEntryMapper;
import org.bremersee.dccon.model.DnsNode;
import org.bremersee.dccon.model.UnknownFilter;

/**
 * The dns node ldap mapper provider.
 *
 * @author Christian Bremer
 */
@FunctionalInterface
public interface DnsNodeLdapMapperProvider {

  /**
   * Gets dns node ldap mapper.
   *
   * @param dnsZoneName the dns zone name
   * @param unknownFilter the unknown filter
   * @return the dns node ldap mapper
   */
  LdaptiveEntryMapper<DnsNode> getDnsNodeLdapMapper(
      String dnsZoneName,
      UnknownFilter unknownFilter);

}
