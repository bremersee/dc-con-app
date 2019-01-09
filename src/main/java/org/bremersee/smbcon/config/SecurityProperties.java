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

package org.bremersee.smbcon.config;

import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The security properties.
 *
 * @author Christian Bremer
 */
@ConfigurationProperties(prefix = "bremersee.access")
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@Slf4j
public class SecurityProperties {

  private static final String ROLE_ACTUATOR = "ROLE_ACTUATOR";

  private static final String IS_AUTHENTICATED = "isAuthenticated()";

  private List<String> ipAddresses = new ArrayList<>();

  private String defaultAccess = "hasIpAddress('127.0.0.1') "
      + "or hasIpAddress('::1') "
      + "or " + IS_AUTHENTICATED;

  /**
   * Build access string.
   *
   * @return the string
   */
  String buildAccess() {
    final StringBuilder sb = new StringBuilder();
    sb.append("hasAuthority('").append(ROLE_ACTUATOR).append("')");
    ipAddresses.forEach(
        ipAddress -> sb.append(" or ").append("hasIpAddress('").append(ipAddress).append("')"));
    final String access = sb.toString();
    log.info("msg=[Actuator access expression created.] expression=[{}]", access);
    return access;
  }

}
