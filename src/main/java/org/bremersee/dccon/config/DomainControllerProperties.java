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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.model.DomainUser;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
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

  public static final Dn MOCK_BASE_DN = new Dn("dc=samdom,dc=example,dc=org");


  private Dn baseDn = new Dn("dc=eixe,dc=bremersee,dc=org");

  private String domainName;


  private UserProperties user = new UserProperties();

  private GroupProperties group = new GroupProperties();

  private ComputerProperties computer = new ComputerProperties();

  private DomainProperties domain = new DomainProperties();


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


  private String dhcpLeaseListBinary = "/usr/sbin/dhcp-lease-list";

  private String dhcpLeaseListExecDir = "/tmp";


  private String nameServerHost = "ns.samdom.example.org";

  private String reverseZoneSuffixIp4 = ".in-addr.arpa";

  private String reverseZoneSuffixIp6 = ".ip6.arpa";

  private List<String> excludedZoneRegexList = new ArrayList<>();

  private List<String> excludedNodeRegexList = new ArrayList<>();


  private String ip4Regex = "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$";

  private String macRegex = "^([0-9A-F]{2}[:-]){5}([0-9A-F]{2})$";


  private String gravatarUrl = "https://www.gravatar.com/avatar/{hash}?d={default}&s={size}"; // TODO mustache or string format


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

  public String getDomainName() {
    if (!isEmpty(domainName)) {
      return getBaseDn().getRDns().stream()
          .filter(rdn -> "dc".equalsIgnoreCase(rdn.getNameValue().getName()))
          .map(rdn -> rdn.getNameValue().getStringValue())
          .collect(Collectors.joining("."));
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
  public static class UserProperties {

    public static final Dn DEFAULT_USER_OU = new Dn("CN=Users");

    private Dn defaultUserOu = DEFAULT_USER_OU;

    private SearchScope defaultUserSearchScope = SearchScope.ONELEVEL;

    /**
     * Specifies whether the username should be used for attribute 'cn' or firstname and lastname.
     */
    private boolean useUsernameAsCn = true;

    private String defaultCompany;

    private String defaultDisplayName = "{{user.firstName}} {{user.lastName}}";

    private String defaultEmail = "{{user.samAccountName}}@{{properties.domainName}}";

    private String defaultGecos = "{{user.firstName}} {{user.lastName}}";

    private Integer defaultGidNumber; // = 100; // = Domain Users

    private String defaultHomeDirectory;

    private String defaultHomeDrive;

    private String defaultLoginShell = "/bin/bash";

    private String defaultNisDomain;

    private String defaultLanguage = "de-DE";

    private String defaultProfilePath;

    private String defaultScriptPath;

    private String defaultUid = "{{user.samAccountName}}";

    private String defaultUnixHomeDirectory = "/home/{{user.samAccountName}}";

    private Map<String, Object> customProperties = new LinkedHashMap<>();

    public void fillDefaults(DomainUser domainUser, boolean rfc2307Enabled) {
      if (isEmpty(domainUser)) {
        return;
      }
      if (isEmpty(domainUser.getCompany())) {
        domainUser.setCompany(getDefaultCompany());
      }
      if (isEmpty(domainUser.getDisplayName())) {
        domainUser.setDisplayName(getDefaultDisplayName());
      }
      if (isEmpty(domainUser.getEmail())) {
        domainUser.setEmail(getDefaultEmail());
      }
      if (isEmpty(domainUser.getHomeDirectory())) {
        domainUser.setHomeDirectory(getDefaultHomeDirectory());
      }
      if (isEmpty(domainUser.getHomeDrive())) {
        domainUser.setHomeDrive(getDefaultHomeDrive());
      }
      if (isEmpty(domainUser.getPreferredLanguage())) {
        domainUser.setPreferredLanguage(getDefaultLanguage());
      }
      if (isEmpty(domainUser.getProfilePath())) {
        domainUser.setProfilePath(getDefaultProfilePath());
      }
      if (isEmpty(domainUser.getScriptPath())) {
        domainUser.setScriptPath(getDefaultScriptPath());
      }
      if (rfc2307Enabled) {
        if (isEmpty(domainUser.getGecos())) {
          domainUser.setGecos(getDefaultGecos());
        }
        if (isEmpty(domainUser.getGidNumber())) {
          domainUser.setGidNumber(getDefaultGidNumber());
        }
        if (isEmpty(domainUser.getLoginShell())) {
          domainUser.setLoginShell(getDefaultLoginShell());
        }
        if (isEmpty(domainUser.getNisDomain())) {
          domainUser.setNisDomain(getDefaultNisDomain());
        }
        if (isEmpty(domainUser.getUid())) {
          domainUser.setUid(getDefaultUid());
        }
        if (isEmpty(domainUser.getUnixHomeDirectory())) {
          domainUser.setUnixHomeDirectory(getDefaultUnixHomeDirectory());
        }
      }
    }

    public void replaceInvalidUsernameWithDefaults(DomainUser domainUser, boolean rfc2307Enabled) {
      if (isEmpty(domainUser) || isEmpty(domainUser.getSamAccountName())) {
        return;
      }
      String username = domainUser.getSamAccountName().toLowerCase();
      if (!isEmpty(domainUser.getDisplayName())
          && domainUser.getDisplayName().toLowerCase().contains(username)) {
        domainUser.setDisplayName(getDefaultDisplayName());
      }
      if (!isEmpty(domainUser.getEmail())
          && domainUser.getEmail().toLowerCase().contains(username)) {
        domainUser.setEmail(getDefaultEmail());
      }
      if (!isEmpty(domainUser.getHomeDirectory())
          && domainUser.getHomeDirectory().toLowerCase().contains(username)) {
        domainUser.setHomeDirectory(getDefaultHomeDirectory());
      }
      if (!isEmpty(domainUser.getScriptPath())
          && domainUser.getScriptPath().toLowerCase().contains(username)) {
        domainUser.setScriptPath(getDefaultScriptPath());
      }
      if (rfc2307Enabled) {
        if (!isEmpty(domainUser.getGecos())
            && domainUser.getGecos().toLowerCase().contains(username)) {
          domainUser.setGecos(getDefaultGecos());
        }
        if (!isEmpty(domainUser.getLoginShell())
            && domainUser.getLoginShell().toLowerCase().contains(username)) {
          domainUser.setLoginShell(getDefaultLoginShell());
        }
        if (!isEmpty(domainUser.getUid())
            && domainUser.getUid().toLowerCase().contains(username)) {
          domainUser.setUid(getDefaultUid());
        }
        if (!isEmpty(domainUser.getUnixHomeDirectory())
            && domainUser.getUnixHomeDirectory().toLowerCase().contains(username)) {
          domainUser.setUnixHomeDirectory(getDefaultUnixHomeDirectory());
        }
      }
    }
    public void replaceNames(DomainUser domainUser, String oldName, String newName) {
      if (isEmpty(domainUser) || isEmpty(oldName) || isEmpty(newName)) {
        return;
      }
      if (!isEmpty(domainUser.getDisplayName())) {
        domainUser.setDisplayName(domainUser.getDisplayName().replace(oldName, newName));
      }
      if (!isEmpty(domainUser.getEmail())) {
        domainUser.setEmail(domainUser.getEmail().replace(oldName, newName));
      }
      if (!isEmpty(domainUser.getGecos())) {
        domainUser.setGecos(domainUser.getGecos().replace(oldName, newName));
      }
      if (!isEmpty(domainUser.getHomeDirectory())) {
        domainUser.setHomeDirectory(domainUser.getHomeDirectory().replace(oldName, newName));
      }
      if (!isEmpty(domainUser.getProfilePath())) {
        domainUser.setProfilePath(domainUser.getProfilePath().replace(oldName, newName));
      }
      if (!isEmpty(domainUser.getScriptPath())) {
        domainUser.setScriptPath(domainUser.getScriptPath().replace(oldName, newName));
      }
      if (!isEmpty(domainUser.getUid())) {
        domainUser.setUid(domainUser.getUid().replace(oldName, newName));
      }
      if (!isEmpty(domainUser.getUnixHomeDirectory())) {
        domainUser.setUnixHomeDirectory(
            domainUser.getUnixHomeDirectory().replace(oldName, newName));
      }
      if (!isEmpty(domainUser.getUserPrincipalName())) {
        domainUser.setUserPrincipalName(
            domainUser.getUserPrincipalName().replace(oldName, newName));
      }
    }
  }

  @Data
  public static class GroupProperties {

    private Dn defaultGroupOu = UserProperties.DEFAULT_USER_OU;

    private SearchScope defaultGroupSearchScope = SearchScope.ONELEVEL;

  }

  @Data
  public static class ComputerProperties {

    public static final Dn DEFAULT_COMPUTER_OU = new Dn("CN=Computers");

    private Dn defaultComputerOu = DEFAULT_COMPUTER_OU;

    private SearchScope defaultComputerSearchScope = SearchScope.ONELEVEL;

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
