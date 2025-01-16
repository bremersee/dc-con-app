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
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
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

  /**
   * Email regex from <a href="https://emailregex.com/">emailregex.com</a> (RFC 5322 Official
   * Standard).
   */
  public static final String EMAIL_REGEX = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+"
      + "(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*"
      + "|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]"
      + "|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+"
      + "[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}"
      + "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:"
      + "(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]"
      + "|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)])";

  public static final int MIN_QUERY_LENGTH = 3; // TODO add to repos and javascript

  public static final Dn MOCK_BASE_DN = new Dn("dc=samdom,dc=example,dc=org");

  public static final Dn[] IGNORED_DN = new Dn[]{
      new Dn("CN=Builtin"),
      new Dn("CN=Configuration"),
      new Dn("CN=Infrastructure"),
      new Dn("CN=LostAndFound"),
      new Dn("CN=NTDS Quotas"),
  };

  private String emailRegex = EMAIL_REGEX;

  private String hostName; // TODO -> samba-tool dns <server>; via command?

  private String domainName;

  private Dn baseDn = new Dn("dc=eixe,dc=bremersee,dc=org");

  @NestedConfigurationProperty
  private CliProperties cli = new CliProperties();

  @NestedConfigurationProperty
  private DomainUserProperties user = new DomainUserProperties();

  @NestedConfigurationProperty
  private DomainGroupProperties group = new DomainGroupProperties();

  @NestedConfigurationProperty
  private DomainComputerProperties computer = new DomainComputerProperties();

  private DomainProperties domain = new DomainProperties(); // move back?


  private String personalName = "Anna Livia";

  private String companyName = "example.org";

  private String companyUrl = "http://example.org";


  /*
  @Deprecated
  private String defaultNisDomain; // = "eixe"; // TODO can I determine it with ldap?, wo hatte ich den gefunden?

  @Deprecated
  private Integer defaultGidNumber; // = 100; // = Domain Users

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
  */



  /*
  @Deprecated
  private String defaultDisplayName = "{{user.firstName}} {{user.lastName}}";

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
  */


  private int maximumPasswordLength = 75;

  private String simplePasswordRegexTemplate = "^(?=.{%d,%d}$).*";

  private String complexPasswordRegexTemplate = "(?=^.{%d,%d}$)"
      + "((?=.*\\d)(?=.*[A-Z])(?=.*[a-z])|(?=.*\\d)(?=.*[^A-Za-z0-9])(?=.*[a-z])"
      + "|(?=.*[^A-Za-z0-9])(?=.*[A-Z])(?=.*[a-z])|(?=.*\\d)(?=.*[A-Z])(?=.*[^A-Za-z0-9]))^.*";


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






  /*
  @Deprecated
  private String defaultLoginShell = "/bin/bash";

  @Deprecated
  private String defaultHomeDrive; // = "H";

  @Deprecated
  private String defaultHomeDirectory = "\\\\data\\home";

  @Deprecated
  private String defaultUnixHomeDirectory = "/home/{{user.samAccountName}}";

  @Deprecated
  private String defaultGecos = "{{user.firstName}} {{user.lastName}}";

  @Deprecated
  private String defaultUid = "{{user.samAccountName}}";
  */



  private String nameServerHost = "ns.samdom.example.org";

  private String reverseZoneSuffixIp4 = ".in-addr.arpa";

  private String reverseZoneSuffixIp6 = ".ip6.arpa";

  private List<String> excludedZoneRegexList = new ArrayList<>();

  private List<String> excludedNodeRegexList = new ArrayList<>();


  private String ip4Regex = "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$";

  private String macRegex = "^([0-9A-F]{2}[:-]){5}([0-9A-F]{2})$";



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

  public String createDefaultUserPrincipalName(String samAccountName) {
    return samAccountName + "@" + createDomainNameFromBaseDn();
  }

  public String createDomainNameFromBaseDn() {
    return getBaseDn().getRDns().stream()
        .filter(rdn -> "dc".equalsIgnoreCase(rdn.getNameValue().getName()))
        .map(rdn -> rdn.getNameValue().getStringValue())
        .collect(Collectors.joining("."));
  }

  public String getDomainName() {
    if (isEmpty(domainName)) {
      return createDomainNameFromBaseDn();
    }
    return domainName;
  }

  public boolean isDn(String value) {
    if (isEmpty(value)) {
      return false;
    }
    try {
      Dn dn = new Dn(value);
      return getBaseDn().isAncestor(dn);

    } catch (RuntimeException e) {
      return false;
    }
  }

  public Dn getBaseDn(Dn ou) {
    if (isEmpty(ou) || ou.isEmpty()) {
      return getBaseDn();
    }
    Dn dn = new Dn(ou.getRDns());
    if (dn.isSame(getBaseDn()) || getBaseDn().isAncestor(dn)) {
      return dn;
    }
    dn.add(getBaseDn());
    return dn;
  }

  public Dn removeBaseDn(Dn dn) {
    Dn baseDn = getBaseDn();
    if (isEmpty(dn) || dn.isEmpty() || dn.isSame(baseDn)) {
      return null;
    }
    if (baseDn.isAncestor(dn)) {
      return dn.subDn(0, dn.size() - baseDn.size());
    }
    return dn;
  }

  public Dn getParentDn(String dn) {
    Dn sourceDn = new Dn(dn);
    if (getBaseDn().isSame(sourceDn)) {
      return sourceDn;
    }
    return sourceDn.getParent();
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

  @Data
  public static class DomainProperties {

    public static final Dn DEFAULT_DOMAIN_CONTROLLERS_OU = new Dn("OU=Domain Controllers");

    public static final Dn DEFAULT_SYSTEM_OU = new Dn("CN=System");

    private Dn defaultSystemOu = DEFAULT_SYSTEM_OU;

    private SearchScope defaultComputerSearchScope = SearchScope.ONELEVEL;

    String defaultNisDomain;
  }

  /**
   * The mail with credentials properties.
   *
   * @author Christian Bremer
   */
  @Data
  public static class MailWithCredentialsProperties {

    private String sender = "no-reply@example.org";

    private String templateBasename = "personal-mail-with-credentials";

    private String loginUrl = "http://localhost:4200/change-password";

    private List<MailInlineAttachment> inlineAttachments = new ArrayList<>();
  }

  /**
   * The mail inline attachment.
   */
  @Data
  public static class MailInlineAttachment {

    private String contentId;

    private String location;

    private String mimeType;
  }
}
