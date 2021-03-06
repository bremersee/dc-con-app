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

  private static final long serialVersionUID = 2L;

  private static final String MIN_LENGTH_PLACEHOLDER = "{{MIN_LENGTH}}";

  private static final String SIMPLE_PASSWORD_REGEX = "^(?=.{" + MIN_LENGTH_PLACEHOLDER
      + ",75}$).*";

  private static final String COMPLEX_PASSWORD_REGEX = "(?=^.{" + MIN_LENGTH_PLACEHOLDER + ",75}$)"
      + "((?=.*\\d)(?=.*[A-Z])(?=.*[a-z])|(?=.*\\d)(?=.*[^A-Za-z0-9])(?=.*[a-z])"
      + "|(?=.*[^A-Za-z0-9])(?=.*[A-Z])(?=.*[a-z])|(?=.*\\d)(?=.*[A-Z])(?=.*[^A-Za-z0-9]))^.*";


  private String personalName = "Anna Livia";

  private String companyName = "example.org";

  private String companyUrl = "http://example.org";


  private String groupBaseDn;

  private String groupRdn = "cn";

  private String groupFindAllFilter = "(objectClass=group)";

  private SearchScope groupFindAllSearchScope = SearchScope.ONELEVEL;

  private String groupFindOneFilter = "(&(objectClass=group)(sAMAccountName={0}))";

  private SearchScope groupFindOneSearchScope = SearchScope.ONELEVEL;


  private String userBaseDn;

  private String userRdn = "cn";

  private String userFindAllFilter = "(objectClass=user)";

  private SearchScope userFindAllSearchScope = SearchScope.ONELEVEL;

  private String userFindOneFilter = "(&(objectClass=user)(sAMAccountName={0}))";

  private SearchScope userFindOneSearchScope = SearchScope.ONELEVEL;


  private String defaultSidPrefix = "S-1-5-21-";

  private int maxSystemSidSuffix = 999;


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


  private String kinitBinary = "/usr/bin/kinit";

  private String kinitAdministratorName = "Administrator";

  private String kinitPasswordFile = "/var/lib/dc-con/dc-pass.txt";

  private String sudoBinary = "/usr/bin/sudo";

  private boolean usingSudo = true;

  private String sambaToolBinary = "/usr/bin/samba-tool";

  private String sambaToolExecDir = "/tmp";

  private String loginShell = "/bin/bash";

  private String homeDirectoryTemplate = "\\\\data\\users\\{}";

  private String unixHomeDirTemplate = "/home/{}";


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
   * Gets simple password regex.
   *
   * @param minLength the min length
   * @return the simple password regex
   */
  public static String getSimplePasswordRegex(int minLength) {
    return SIMPLE_PASSWORD_REGEX.replace(MIN_LENGTH_PLACEHOLDER, String.valueOf(minLength));
  }

  /**
   * Gets complex password regex.
   *
   * @param minLength the min length
   * @return the complex password regex
   */
  public static String getComplexPasswordRegex(int minLength) {
    return COMPLEX_PASSWORD_REGEX.replace(MIN_LENGTH_PLACEHOLDER, String.valueOf(minLength));
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
