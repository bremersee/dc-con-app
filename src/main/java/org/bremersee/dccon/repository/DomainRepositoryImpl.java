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

package org.bremersee.dccon.repository;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.PasswordInformation;
import org.bremersee.dccon.repository.cli.CommandExecutor;
import org.bremersee.dccon.repository.cli.PasswordInformationParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * The domain repository implementation.
 *
 * @author Christian Bremer
 */
@Profile("ldap")
@Component("domainRepository")
@Slf4j
public class DomainRepositoryImpl extends AbstractRepository implements DomainRepository {

  private PasswordInformationParser passwordInformationParser;

  /**
   * Instantiates a domain repository.
   *
   * @param properties the properties
   */
  public DomainRepositoryImpl(final DomainControllerProperties properties) {
    super(properties, null);
    this.passwordInformationParser = PasswordInformationParser.defaultParser();
  }

  /**
   * Sets password information parser.
   *
   * @param passwordInformationParser the password information parser
   */
  @Autowired(required = false)
  public void setPasswordInformationParser(
      final PasswordInformationParser passwordInformationParser) {
    if (passwordInformationParser != null) {
      this.passwordInformationParser = passwordInformationParser;
    }
  }

  @Cacheable(cacheNames = "password-information")
  @Override
  public PasswordInformation getPasswordInformation() {
    kinit();
    final List<String> commands = new ArrayList<>();
    sudo(commands);
    commands.add(getProperties().getSambaToolBinary());
    commands.add("domain");
    commands.add("passwordsettings");
    commands.add("show");
    auth(commands);
    return CommandExecutor.exec(
        commands,
        null,
        getProperties().getSambaToolExecDir(),
        passwordInformationParser);
  }
}
