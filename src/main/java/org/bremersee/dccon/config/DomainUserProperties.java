/*
 * Copyright 2025 the original author or authors.
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

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.bremersee.dccon.controller.ui.model.DomainUserAddRequest;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.model.TreeSearchScope;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;

/**
 * The type DomainUserProperties.
 *
 * @author Christian Bremer
 */
@Data
public class DomainUserProperties {

  public static final Dn DEFAULT_OU = new Dn("CN=Users");

  @NotNull
  private Dn defaultOu = DEFAULT_OU;

  @NotNull
  private TreeSearchScope defaultSearchScope = TreeSearchScope.ONELEVEL;

  /**
   * Specifies whether the username should be used for attribute 'cn' or firstname and lastname.
   */
  private boolean useUsernameAsCn = true;

  private String defaultCompany;

  private boolean disableCompany; // TODO subclass for ui that i can directly use in controller?

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

  // TODO move to user edit controller
  public void replaceNames(DomainUser domainUser, String oldName, String newName) {
    if (isEmpty(domainUser) || isEmpty(oldName)) {
      return;
    }
    String replacement = isEmpty(newName) ? "" : newName;
    if (!isEmpty(domainUser.getDisplayName())) {
      domainUser.setDisplayName(domainUser.getDisplayName().replace(oldName, replacement).trim());
    }
    if (!isEmpty(domainUser.getEmail())) {
      domainUser.setEmail(domainUser.getEmail().replace(oldName, replacement).trim());
    }
    if (!isEmpty(domainUser.getGecos())) {
      domainUser.setGecos(domainUser.getGecos().replace(oldName, replacement).trim());
    }
    if (!isEmpty(domainUser.getHomeDirectory())) {
      domainUser.setHomeDirectory(
          domainUser.getHomeDirectory().replace(oldName, replacement).trim());
    }
    if (!isEmpty(domainUser.getProfilePath())) {
      domainUser.setProfilePath(domainUser.getProfilePath().replace(oldName, replacement).trim());
    }
    if (!isEmpty(domainUser.getScriptPath())) {
      domainUser.setScriptPath(domainUser.getScriptPath().replace(oldName, replacement).trim());
    }
    if (!isEmpty(domainUser.getUid())) {
      domainUser.setUid(domainUser.getUid().replace(oldName, replacement).trim());
    }
    if (!isEmpty(domainUser.getUnixHomeDirectory())) {
      domainUser.setUnixHomeDirectory(
          domainUser.getUnixHomeDirectory().replace(oldName, replacement).trim());
    }
    if (!isEmpty(domainUser.getUserPrincipalName())) {
      domainUser.setUserPrincipalName(
          domainUser.getUserPrincipalName().replace(oldName, replacement).trim());
    }
  }
}
