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

package org.bremersee.smbcon.business;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.smbcon.LdaptiveProperties;
import org.bremersee.smbcon.SambaDomainProperties;
import org.bremersee.smbcon.model.DnsEntry;
import org.bremersee.smbcon.model.DnsRecordType;
import org.bremersee.smbcon.model.DnsZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author Christian Bremer
 */
@Component
@Slf4j
public class SambaToolImpl implements SambaTool {

  //samba-tool nameserver query dc.eixe.bremersee.org eixe.bremersee.org @ ALL --simple-bind-dn="cn=administrator,cn=users,dc=eixe,dc=bremersee,dc=org" --password="{}"

  private static final String SUB_CMD_USER_MANAGEMENT = "user";


  private static final String USER_CMD_CREATE = "create";

  private static final String USER_OPTION_GECOS = "--gecos='{}'";

  private static final String USER_OPTION_UNIX_UID = "--uid='{}'";

  private static final String USER_OPTION_UNIX_HOME = "--unix-home='{}'";

  private static final String USER_OPTION_UNIX_SHELL = "--login-shell='{}'";

  private static final String USER_OPTION_MAIL_ADDRESS = "--mail-address='{}'";

  private static final String USER_OPTION_MOBILE = "--telephone-number='{}'";


  private static final String USER_CMD_DELETE = "delete";

  private static final String USER_CMD_SET_PASSWORD = "setpassword"; // NOSONAR

  private static final String USER_OPTION_NEW_PASSWORD = "--newpassword={}"; // NOSONAR


  private static final String SUB_CMD_GROUP_MANAGEMENT = "group";

  private static final String GROUP_CMD_ADD = "add";

  private static final String GROUP_CMD_DELETE = "delete";


  private static final String SUB_CMD_DNS_MANAGEMENT = "nameserver";


  private static final String DNS_CMD_ZONELIST = "zonelist";

  private static final String DNS_CMD_ZONECREATE = "zonecreate";

  private static final String DNS_CMD_ZONEDELETE = "zonedelete";

  private static final String DNS_CMD_QUERY = "query";

  private static final String DNS_CMD_ADD = "add";

  private static final String DNS_CMD_DELETE = "delete";

  private static final String DNS_CMD_UPDATE = "update";


  private static final String DNS_SIMPLE_BIND_DN = "--simple-bind-dn=\"{}\"";

  private static final String DNS_SIMPLE_BIND_PASSWORD = "--password=\"{}\""; // NOSONAR


  private final SambaDomainProperties properties;

  private final LdaptiveProperties adProperties;

  private final SambaToolResponseParser responseParser;

  @Autowired
  public SambaToolImpl(
      final SambaDomainProperties properties,
      final LdaptiveProperties adProperties,
      final SambaToolResponseParser responseParser) {
    this.properties = properties;
    this.adProperties = adProperties;
    this.responseParser = responseParser;
  }

  @Override
  public List<DnsZone> getDnsZones() {

    final List<String> commands = new ArrayList<>();
    if (properties.isUsingSudo()) {
      commands.add(properties.getSudoBinary());
    }
    commands.add(properties.getSambaToolBinary());
    commands.add(SUB_CMD_DNS_MANAGEMENT);
    commands.add(DNS_CMD_ZONELIST);
    commands.add(properties.getNameServerHost());
    commands.add(DNS_SIMPLE_BIND_DN.replace("{}", adProperties.getBindDn()));
    commands.add(DNS_SIMPLE_BIND_PASSWORD.replace("{}", adProperties.getBindCredential()));

    final CommandExecutorResponse response = CommandExecutor.exec(
        commands, properties.getSambaToolExecDir());
    return responseParser.parseDnsZones(response);
  }

  @Override
  public void createDnsZone(@NotNull final String zoneName) {
    execDnsZoneCmd(DNS_CMD_ZONECREATE, zoneName);
  }

  @Override
  public void deleteDnsZone(@NotNull final String zoneName) {
    execDnsZoneCmd(DNS_CMD_ZONEDELETE, zoneName);
  }

  private void execDnsZoneCmd(
      @NotNull final String dnsCommand,
      @NotNull final String zoneName) {

    final List<String> commands = new ArrayList<>();
    if (properties.isUsingSudo()) {
      commands.add(properties.getSudoBinary());
    }
    commands.add(properties.getSambaToolBinary());
    commands.add(SUB_CMD_DNS_MANAGEMENT);
    commands.add(dnsCommand);
    commands.add(properties.getNameServerHost());
    commands.add(zoneName);
    commands.add(DNS_SIMPLE_BIND_DN.replace("{}", adProperties.getBindDn()));
    commands.add(DNS_SIMPLE_BIND_PASSWORD.replace("{}", adProperties.getBindCredential()));

    final CommandExecutorResponse response = CommandExecutor.exec(
        commands, properties.getSambaToolExecDir());
    if (StringUtils.hasText(response.getError())) {
      log.warn("Executing '{}' zone name '{}' failed:\n{}",
          dnsCommand, zoneName, response.getOutput());
    } else {
      log.info("Executing '{}' zone name '{}' with response:\n{}",
          dnsCommand, zoneName, response.getOutput());
    }
  }

  @Override
  public List<DnsEntry> getDnsRecords(@NotNull final String zoneName) {

    final List<String> commands = new ArrayList<>();
    if (properties.isUsingSudo()) {
      commands.add(properties.getSudoBinary());
    }
    commands.add(properties.getSambaToolBinary());
    commands.add(SUB_CMD_DNS_MANAGEMENT);
    commands.add(DNS_CMD_QUERY);
    commands.add(properties.getNameServerHost());
    commands.add(zoneName);
    commands.add("@");
    commands.add("ALL");
    commands.add(DNS_SIMPLE_BIND_DN.replace("{}", adProperties.getBindDn()));
    commands.add(DNS_SIMPLE_BIND_PASSWORD.replace("{}", adProperties.getBindCredential()));

    final CommandExecutorResponse response = CommandExecutor.exec(
        commands, properties.getSambaToolExecDir());
    return responseParser.parseDnsRecords(response);
  }

  @Override
  public void addDnsRecord(
      @NotNull final String zoneName,
      @NotNull final String name,
      @NotNull final DnsRecordType recordType,
      @NotNull final String data) {

    execDnsRecordCmd(DNS_CMD_ADD, zoneName, name, recordType, data, null);
  }

  @Override
  public void deleteDnsRecord(
      @NotNull final String zoneName,
      @NotNull final String name,
      @NotNull final DnsRecordType recordType,
      @NotNull final String data) {

    execDnsRecordCmd(DNS_CMD_DELETE, zoneName, name, recordType, data, null);
  }

  @Override
  public void updateDnsRecord(
      @NotNull final String zoneName,
      @NotNull final String name,
      @NotNull final DnsRecordType recordType,
      @NotNull final String oldData,
      @NotNull final String newData) {

    execDnsRecordCmd(DNS_CMD_UPDATE, zoneName, name, recordType, oldData, newData);
  }

  private void execDnsRecordCmd(
      @NotNull final String dnsCommand,
      @NotNull final String zoneName,
      @NotNull final String name,
      @NotNull final DnsRecordType recordType,
      @NotNull final String data,
      final String newData) {

    final List<String> commands = new ArrayList<>();
    if (properties.isUsingSudo()) {
      commands.add(properties.getSudoBinary());
    }
    commands.add(properties.getSambaToolBinary());
    commands.add(SUB_CMD_DNS_MANAGEMENT);
    commands.add(dnsCommand);
    commands.add(properties.getNameServerHost());
    commands.add(zoneName);
    commands.add(name);
    commands.add(recordType.name());
    commands.add(data);
    if (DNS_CMD_UPDATE.equals(dnsCommand) && StringUtils.hasText(newData)) {
      commands.add(newData);
    }
    commands.add(DNS_SIMPLE_BIND_DN.replace("{}", adProperties.getBindDn()));
    commands.add(DNS_SIMPLE_BIND_PASSWORD.replace("{}", adProperties.getBindCredential()));

    final CommandExecutorResponse response = CommandExecutor.exec(
        commands, properties.getSambaToolExecDir());
    if (StringUtils.hasText(response.getError())) {
      log.warn("Executing '{}' {} Record of hostname {} failed:\n{}",
          dnsCommand, recordType, name, response.getOutput());
    } else {
      log.info("Executing '{}' {} Record of hostname {} with response:\n{}",
          dnsCommand, recordType, name, response.getOutput());
    }
  }

  @Override
  public void createUser(
      @NotNull final String userName,
      @NotNull final String password,
      final String displayName,
      final String email,
      final String mobile) {

    final String unixHomeDir = properties.getUnixHomeDirTemplate().replace("{}", userName);
    final List<String> commands = new ArrayList<>();
    if (properties.isUsingSudo()) {
      commands.add(properties.getSudoBinary());
    }
    commands.add(properties.getSambaToolBinary());
    commands.add(SUB_CMD_USER_MANAGEMENT);
    commands.add(USER_CMD_CREATE);
    commands.add(userName);
    commands.add(password);
    commands.add(USER_OPTION_UNIX_UID.replace("{}", userName));
    commands.add(USER_OPTION_UNIX_HOME.replace("{}", unixHomeDir));
    commands.add(USER_OPTION_UNIX_SHELL.replace("{}", properties.getLoginShell()));
    if (StringUtils.hasText(displayName)) {
      commands.add(USER_OPTION_GECOS.replace("{}", displayName));
    }
    if (StringUtils.hasText(email)) {
      commands.add(USER_OPTION_MAIL_ADDRESS.replace("{}", email));
    }
    if (StringUtils.hasText(mobile)) {
      commands.add(USER_OPTION_MOBILE.replace("{}", mobile));
    }

    final CommandExecutorResponse response = CommandExecutor.exec(
        commands, properties.getSambaToolExecDir());
    if (StringUtils.hasText(response.getError())) {
      log.warn("Creating user {} with error:\n{}", userName, response.getOutput());
    } else {
      log.info("Creating user {} with response:\n{}", userName, response.getOutput());
    }
  }

  @Override
  public void deleteUser(@NotNull final String userName) {

    final List<String> commands = new ArrayList<>();
    if (properties.isUsingSudo()) {
      commands.add(properties.getSudoBinary());
    }
    commands.add(properties.getSambaToolBinary());
    commands.add(SUB_CMD_USER_MANAGEMENT);
    commands.add(USER_CMD_DELETE);
    commands.add(userName);

    final CommandExecutorResponse response = CommandExecutor.exec(
        commands, properties.getSambaToolExecDir());
    if (StringUtils.hasText(response.getError())) {
      log.warn("Deleting user {} with error:\n{}", userName, response.getOutput());
    } else {
      log.info("Deleting user {} with response:\n{}", userName, response.getOutput());
    }
  }

  @Override
  public void setNewPassword(
      @NotNull final String userName,
      @NotNull final String newPassword) {

    final List<String> commands = new ArrayList<>();
    if (properties.isUsingSudo()) {
      commands.add(properties.getSudoBinary());
    }
    commands.add(properties.getSambaToolBinary());
    commands.add(SUB_CMD_USER_MANAGEMENT);
    commands.add(USER_CMD_SET_PASSWORD);
    commands.add(userName);
    commands.add(USER_OPTION_NEW_PASSWORD.replace("{}", newPassword));

    final CommandExecutorResponse response = CommandExecutor.exec(
        commands, properties.getSambaToolExecDir());
    if (StringUtils.hasText(response.getError())) {
      log.warn("Setting new password of user {} with error:\n{}",
          userName, response.getOutput());
    } else {
      log.info("Setting new password of user {} with response:\n{}",
          userName, response.getOutput());
    }
  }

  @Override
  public void addGroup(String groupName) {

    final List<String> commands = new ArrayList<>();
    if (properties.isUsingSudo()) {
      commands.add(properties.getSudoBinary());
    }
    commands.add(properties.getSambaToolBinary());
    commands.add(SUB_CMD_GROUP_MANAGEMENT);
    commands.add(GROUP_CMD_ADD);
    commands.add(groupName);

    final CommandExecutorResponse response = CommandExecutor.exec(
        commands, properties.getSambaToolExecDir());
    if (StringUtils.hasText(response.getError())) {
      log.warn("Adding group {} with error:\n{}",
          groupName, response.getOutput());
    } else {
      log.info("Adding group {} with response:\n{}",
          groupName, response.getOutput());
    }
  }

  @Override
  public void deleteGroup(String groupName) {

    final List<String> commands = new ArrayList<>();
    if (properties.isUsingSudo()) {
      commands.add(properties.getSudoBinary());
    }
    commands.add(properties.getSambaToolBinary());
    commands.add(SUB_CMD_GROUP_MANAGEMENT);
    commands.add(GROUP_CMD_DELETE);
    commands.add(groupName);

    final CommandExecutorResponse response = CommandExecutor.exec(
        commands, properties.getSambaToolExecDir());
    if (StringUtils.hasText(response.getError())) {
      log.warn("Deleting group {} with error:\n{}",
          groupName, response.getOutput());
    } else {
      log.info("Deleting group {} with response:\n{}",
          groupName, response.getOutput());
    }
  }

}
