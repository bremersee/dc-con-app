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

package org.bremersee.dccon.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.ldaptive.SearchScope;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * The samba domain properties.
 *
 * @author Christian Bremer
 */
@ConfigurationProperties(prefix = "bremersee.domain-controller")
@Component
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Slf4j
public class DomainControllerProperties implements Serializable {

  private static final long serialVersionUID = 606284794541721895L;

  private String groupBaseDn;

  private String groupRdn = "cn";

  private String groupMemberAttr = "member";

  private String groupFindAllFilter = "(objectClass=group)";

  private SearchScope groupFindAllSearchScope = SearchScope.ONELEVEL;

  private String groupFindOneFilter = "(&(objectClass=group)(sAMAccountName={0}))";

  private SearchScope groupFindOneSearchScope = SearchScope.ONELEVEL;

  private String userBaseDn;

  private String userRdn = "cn";

  private String userGroupAttr = "memberOf";

  private String userFindAllFilter = "(objectClass=user)";

  private SearchScope userFindAllSearchScope = SearchScope.ONELEVEL;

  private String userFindOneFilter = "(&(objectClass=user)(sAMAccountName={0}))";

  private SearchScope userFindOneSearchScope = SearchScope.ONELEVEL;

  private String kinitBinary = "/usr/bin/kinit";

  private String kinitAdministratorName = "Administrator";

  private String kinitPasswordFile = "/var/lib/dc-con/dc-pass.txt";

  private String sudoBinary = "/usr/bin/sudo";

  private boolean usingSudo = true;

  private String sambaToolBinary = "/usr/bin/samba-tool";

  private String sambaToolExecDir = "/tmp";

  private String loginShell = "/bin/bash";

  private String unixHomeDirTemplate = "/home/{}";


  private String dhcpLeaseListBinary = "/usr/sbin/dhcp-lease-list";

  private String dhcpLeaseListExecDir = "/tmp";


  private String nameServerHost = "ns.example.org";

  private String reverseZoneSuffix = ".in-addr.arpa";

  private List<String> excludedZoneRegexList = new ArrayList<>();

  private List<String> excludedEntryRegexList = new ArrayList<>();


  private Map<String, List<String>> dnsZoneMapping = new LinkedHashMap<>();


  /**
   * Instantiates a new Domain controller properties.
   */
  public DomainControllerProperties() {
    excludedZoneRegexList.add("^_msdcs\\..*$");

    excludedEntryRegexList.add("^$");
    excludedEntryRegexList.add("_msdcs");
    excludedEntryRegexList.add("_sites");
    excludedEntryRegexList.add("_tcp");
    excludedEntryRegexList.add("_udp");
  }

  /**
   * Init.
   */
  @PostConstruct
  public void init() {
    if (dnsZoneMapping != null) {
      for (String key : dnsZoneMapping.keySet()) {
        while (dnsZoneMapping.get(key) != null && dnsZoneMapping.get(key).remove(key)) {
          log.info(
              "msg=[Value of dns zone mapping cannot equals key. Value was removed.] value=[{}]",
              key);
        }
      }
    }
    log.info("msg=[Domain controller properties loaded.] properties=[{}]", this);
  }

  /**
   * Find correlated dns zones.
   *
   * @param zoneName the zone name
   * @return the correlated dns zones
   */
  public Set<String> findCorrelatedDnsZones(String zoneName) {
    if (!StringUtils.hasText(zoneName)) {
      return Collections.emptySet();
    }
    List<String> correlatedZones = dnsZoneMapping.get(zoneName);
    if (correlatedZones != null && !correlatedZones.isEmpty()) {
      return new LinkedHashSet<>(correlatedZones);
    }
    final Set<String> results = new LinkedHashSet<>();
    for (Map.Entry<String, List<String>> entry : dnsZoneMapping.entrySet()) {
      if (entry.getValue() != null && entry.getValue().contains(zoneName)) {
        results.add(entry.getKey());
      }
    }
    return results;
  }

}
