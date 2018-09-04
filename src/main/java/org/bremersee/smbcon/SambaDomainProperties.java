/*
 * Copyright 2017 the original author or authors.
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

package org.bremersee.smbcon;

import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.bremersee.smbcon.model.Info;
import org.ldaptive.SearchScope;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Christian Bremer
 */
@ConfigurationProperties(prefix = "bremersee.samba-domain")
@Component
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class SambaDomainProperties implements Serializable {

  private static final long serialVersionUID = 606284794541721895L;

  private String groupBaseDn;

  private String groupRdn = "cn";

  private String groupMemberAttr = "member";

  private boolean memberDn = true;

  private String groupFindAllFilter = "(objectClass=group)";

  private SearchScope groupFindAllSearchScope = SearchScope.ONELEVEL;

  private String groupFindOneFilter = "(&(objectClass=group)(sAMAccountName={0}))";

  private SearchScope groupFindOneSearchScope = SearchScope.ONELEVEL;

  private String userBaseDn;

  private String userRdn = "cn";

  private String userGroupAttr = "memberOf";

  private boolean userGroupDn = true;

  private String userFindOneFilter = "(&(objectClass=user)(sAMAccountName={0}))";

  private SearchScope userFindOneSearchScope = SearchScope.ONELEVEL;

  private String kinitBinary = "/usr/bin/kinit";

  private String kinitDevNull = "2&>/dev/null";

  private boolean usingKinit = true;

  private String kinitPasswordFile = "/var/lib/smb-con/smb-pass.txt";

  private String sudoBinary = "/usr/bin/sudo";

  private boolean usingSudo = true;

  private String sambaToolBinary = "/usr/bin/samba-tool";

  private String sambaToolExecDir = "/tmp";

  private String loginShell = "/bin/bash";

  private String unixHomeDirTemplate = "/home/{}";


  private String nameServerHost = "ns.example.org";


  public Info buildInfo() {
    return new Info()
        .nameServerHost(nameServerHost);
  }

}
