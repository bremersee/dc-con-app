/*
 * Copyright 2024 the original author or authors.
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

package org.bremersee.dccon.model;

import static java.util.Objects.isNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * The type DomainComputer.
 *
 * @author Christian Bremer
 */
@Schema(description = "Domain computer.")
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class DomainComputer extends SamAccount {

  String name;

  String dnsHostName;

  List<String> networkAddresses;

  String operatingSystem;

  String operatingSystemVersion;

  String description;

  List<String> servicePrincipalNames;

  Boolean criticalSystemObject;

  public List<String> getNetworkAddresses() {
    if (isNull(networkAddresses)) {
      networkAddresses = new ArrayList<>();
    }
    return networkAddresses;
  }

  public List<String> getServicePrincipalNames() {
    if (isNull(servicePrincipalNames)) {
      servicePrincipalNames = new ArrayList<>();
    }
    return servicePrincipalNames;
  }

  public Boolean getCriticalSystemObject() {
    return Boolean.TRUE.equals(criticalSystemObject);
  }

  @Hidden
  @JsonIgnore
  public String getSamAccountNameWithoutTrailingDollarSign() {
    if (isNull(samAccountName) || samAccountName.isEmpty()) {
      return null;
    }
    return samAccountName.substring(0, samAccountName.length() - 1);
  }
}
