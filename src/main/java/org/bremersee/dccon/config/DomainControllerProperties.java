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

package org.bremersee.dccon.config;

import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.ldaptive.SearchScope;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * The domain controller properties.
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

  @Serial
  private static final long serialVersionUID = 3L;

  public static final String MOCK_BASE_DN = "dc=samdom,dc=example,dc=org";

  public static final String DEFAULT_USER_OU = "CN=Users";

  public static final String DEFAULT_COMPUTER_OU = "CN=Computers";

  public static final String DEFAULT_DOMAIN_CONTROLLERS_OU = "OU=Domain Controllers";


  private String personalName = "Anna Livia";

  private String companyName = "example.org";

  private String companyUrl = "http://example.org";


  private String defaultNisDomain; // = "eixe"; // TODO can I determine it with ldap?

  private Integer defaultGidNumber; // = 100; // = Domain Users


  private String baseDn = "dc=eixe,dc=bremersee,dc=org";


  private String defaultGroupOu = "CN=Users";

  private SearchScope defaultGroupSearchScope = SearchScope.ONELEVEL;


  @Deprecated
  private String groupBaseDn;

  @Deprecated
  private String groupRdn = "cn";

  @Deprecated
  private String groupFindAllFilter = "(objectClass=group)"; // deprecated

  @Deprecated
  private SearchScope groupFindAllSearchScope = SearchScope.ONELEVEL;

  @Deprecated
  private String groupFindOneFilter = "(&(objectClass=group)(cn={0}))";

  @Deprecated
  private SearchScope groupFindOneSearchScope = SearchScope.ONELEVEL;


  private String defaultComputerOu = DEFAULT_COMPUTER_OU;

  private SearchScope defaultComputerSearchScope = SearchScope.ONELEVEL;


  /**
   * Specifies whether the username should be used for attribute 'cn' or firstname and lastname.
   */
  private boolean useUsernameAsCn = true;

  private String defaultUserOu = DEFAULT_USER_OU;

  private SearchScope defaultUserSearchScope = SearchScope.ONELEVEL;

  @Deprecated
  private String userBaseDn;

  @Deprecated
  private String userRdn = "cn";

  @Deprecated
  private String userFindAllFilter = "(objectClass=user)"; // deprecated

  @Deprecated
  private SearchScope userFindAllSearchScope = SearchScope.ONELEVEL;

  @Deprecated
  private String userFindOneFilter = "(&(objectClass=user)(sAMAccountName={0}))";

  @Deprecated
  private String userFindOneFilterByUsernameOrEmail = "(&(objectClass=user)(|(sAMAccountName={0})(mail={0})))";

  @Deprecated
  private SearchScope userFindOneSearchScope = SearchScope.ONELEVEL;


  private int maximumPasswordLength = 75;

  private String simplePasswordRegexTemplate = "^(?=.{%d,%d}$).*";

  private String complexPasswordRegexTemplate = "(?=^.{%d,%d}$)"
      + "((?=.*\\d)(?=.*[A-Z])(?=.*[a-z])|(?=.*\\d)(?=.*[^A-Za-z0-9])(?=.*[a-z])"
      + "|(?=.*[^A-Za-z0-9])(?=.*[A-Z])(?=.*[a-z])|(?=.*\\d)(?=.*[A-Z])(?=.*[^A-Za-z0-9]))^.*";

  /**
   * ISO 639-1 language codes. The combinations like de-DE and en-US with ISO-639 and ISO-3166 also
   * work.
   */
  private String defaultPreferredLanguage = "de";

  //private String defaultSidPrefix = "S-1-5-21-";

  //private int maxSystemSidSuffix = 999;


  private String dnsZoneBaseDn;

  private String dnsZoneRdn = "dc";

  private String dnsZoneFindAllFilter = "(objectClass=dnsZone)";

  private SearchScope dnsZoneFindAllSearchScope = SearchScope.SUBTREE;

  private String dnsZoneFindOneFilter = "(&(objectClass=dnsZone)(name={0}))";

  private SearchScope dnsZoneFindOneSearchScope = SearchScope.SUBTREE;


  private String defaultZone = "samdom.example.org";

  private String dnsNodeBaseDn;

  private String dnsNodeRdn = "dc";

  private String dnsNodeFindAllFilter = "(objectClass=dnsNode)";

  private SearchScope dnsNodeFindAllSearchScope = SearchScope.SUBTREE;

  private String dnsNodeFindOneFilter = "(&(objectClass=dnsNode)(name={0}))";

  private SearchScope dnsNodeFindOneSearchScope = SearchScope.SUBTREE;


  private boolean usingKinit = false;

  private String kinitBinary = "/usr/bin/kinit";

  private String kinitAdministratorName = "Administrator";

  private String kinitPasswordFile = "/var/lib/dc-con/dc-pass.txt";


  private boolean usingSudo = true;

  private String sudoBinary = "/usr/bin/sudo";


  private boolean usingSsh = false;

  private String sshCommand = "/usr/bin/ssh root@dc1";


  private String sambaToolBinary = "/usr/bin/samba-tool";

  private String sambaToolExecDir = "/tmp";

  // TODO move to rfc class
  private String loginShell = "/bin/bash";

  private String homeDirectoryTemplate = "\\\\data\\users\\{}"; // TODO use %s

  private String unixHomeDirTemplate = "/home/{}"; // TODO use %s


  private String dhcpLeaseListBinary = "/usr/sbin/dhcp-lease-list";

  private String dhcpLeaseListExecDir = "/tmp";


  private String nameServerHost = "ns.samdom.example.org";

  private String reverseZoneSuffixIp4 = ".in-addr.arpa";

  private String reverseZoneSuffixIp6 = ".ip6.arpa";

  private List<String> excludedZoneRegexList = new ArrayList<>();

  private List<String> excludedNodeRegexList = new ArrayList<>();


  private String ip4Regex = "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$";

  private String macRegex = "^([0-9A-F]{2}[:-]){5}([0-9A-F]{2})$";


  private String gravatarUrl = "https://www.gravatar.com/avatar/{hash}?d={default}&s={size}";


  private MailWithCredentialsProperties mailWithCredentials = new MailWithCredentialsProperties();


  /**
   * Instantiates a new Domain controller properties.
   */
  public DomainControllerProperties() {
    excludedZoneRegexList.add("^_msdcs\\..*$");
    excludedZoneRegexList.add("RootDNSServers");

    excludedNodeRegexList.add("^$");
    excludedNodeRegexList.add("_msdcs");
    excludedNodeRegexList.add("_sites");
    excludedNodeRegexList.add("_tcp");
    excludedNodeRegexList.add("_udp");

    excludedNodeRegexList.add("@");
    excludedNodeRegexList.add("_gc\\..*$");
    excludedNodeRegexList.add("_kerberos\\..*$");
    excludedNodeRegexList.add("_kpasswd\\..*$");
    excludedNodeRegexList.add("_ldap\\..*$");
    excludedNodeRegexList.add("ForestDnsZones");
  }

  public String getBaseDn() {
    if (isEmpty(baseDn)) {
      return MOCK_BASE_DN;
    }
    return baseDn;
  }

  public String getDefaultGroupOu() {
    if (isEmpty(defaultGroupOu)) {
      return DEFAULT_USER_OU;
    }
    return defaultGroupOu;
  }

  public SearchScope getDefaultGroupSearchScope() {
    if (isEmpty(defaultGroupSearchScope)) {
      return SearchScope.ONELEVEL;
    }
    return defaultGroupSearchScope;
  }

  public String getDefaultUserOu() {
    if (isEmpty(defaultUserOu)) {
      return DEFAULT_USER_OU;
    }
    return defaultUserOu;
  }

  public SearchScope getDefaultUserSearchScope() {
    if (isEmpty(defaultUserSearchScope)) {
      return SearchScope.ONELEVEL;
    }
    return defaultUserSearchScope;
  }

  public String getDefaultComputerOu() {
    if (isEmpty(defaultComputerOu)) {
      return DEFAULT_COMPUTER_OU;
    }
    return defaultComputerOu;
  }

  public SearchScope getDefaultComputerSearchScope() {
    if (isEmpty(defaultComputerSearchScope)) {
      return SearchScope.ONELEVEL;
    }
    return defaultComputerSearchScope;
  }

  /**
   * Gets reverse zone suffix list.
   *
   * @return the reverse zone suffix list
   */
  public List<String> getReverseZoneSuffixList() {
    return Arrays.asList(reverseZoneSuffixIp4, reverseZoneSuffixIp6);
  }

  /**
   * Determines whether the given zone is a reverse zone or not.
   *
   * @param zoneName the zone name
   * @return {@code true} if the zone is a reverse zone, otherwise {@code false}
   */
  public boolean isReverseZone(final String zoneName) {
    return zoneName != null && getReverseZoneSuffixList().stream()
        .anyMatch(suffix -> zoneName.toLowerCase().endsWith(suffix.toLowerCase()));
  }

  /**
   * Build dns node base dn string.
   *
   * @param zoneName the zone name
   * @return the string
   */
  public String buildDnsNodeBaseDn(String zoneName) {
    return dnsNodeBaseDn.replace("{zoneName}", zoneName);
  }

  /**
   * The mail with credentials properties.
   *
   * @author Christian Bremer
   */
  @Getter
  @Setter
  @ToString
  @EqualsAndHashCode
  @NoArgsConstructor
  @SuppressWarnings("WeakerAccess")
  public static class MailWithCredentialsProperties {

    private String sender = "no-reply@example.org";

    private String templateBasename = "personal-mail-with-credentials";

    private String loginUrl = "http://localhost:4200/change-password";

    private List<MailInlineAttachment> inlineAttachments = new ArrayList<>();
  }

  /**
   * The mail inline attachment.
   */
  @Getter
  @Setter
  @ToString
  @EqualsAndHashCode
  @NoArgsConstructor
  public static class MailInlineAttachment {

    private String contentId;

    private String location;

    private String mimeType;
  }
}
