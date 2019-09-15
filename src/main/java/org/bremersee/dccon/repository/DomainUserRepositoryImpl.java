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
import java.util.Optional;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.data.ldaptive.LdaptiveTemplate;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.repository.cli.CommandExecutor;
import org.bremersee.dccon.repository.cli.CommandExecutorResponse;
import org.bremersee.dccon.repository.cli.CommandExecutorResponseValidator;
import org.bremersee.dccon.repository.ldap.DomainUserLdapMapper;
import org.bremersee.exception.ServiceException;
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * The domain user repository.
 *
 * @author Christian Bremer
 */
@Profile("ldap")
@Component("domainUserRepository")
@Slf4j
public class DomainUserRepositoryImpl extends AbstractRepository implements DomainUserRepository {

  private DomainUserLdapMapper domainUserLdapMapper;

  /**
   * Instantiates a new domain user repository.
   *
   * @param properties   the properties
   * @param ldapTemplate the ldap template
   */
  public DomainUserRepositoryImpl(
      DomainControllerProperties properties,
      LdaptiveTemplate ldapTemplate) {
    super(properties, ldapTemplate);
    domainUserLdapMapper = new DomainUserLdapMapper(properties);
  }

  /**
   * Sets domain user ldap mapper.
   *
   * @param domainUserLdapMapper the domain user ldap mapper
   */
  @Autowired(required = false)
  public void setDomainUserLdapMapper(
      DomainUserLdapMapper domainUserLdapMapper) {
    if (domainUserLdapMapper != null) {
      this.domainUserLdapMapper = domainUserLdapMapper;
    }
  }

  @Override
  public Stream<DomainUser> findAll() {
    final SearchRequest searchRequest = new SearchRequest(
        getProperties().getUserBaseDn(),
        new SearchFilter(getProperties().getUserFindAllFilter()));
    searchRequest.setSearchScope(getProperties().getUserFindAllSearchScope());
    return getLdapTemplate().findAll(searchRequest, domainUserLdapMapper);
  }

  @Override
  public Optional<DomainUser> findOne(@NotNull String userName) {
    final SearchFilter searchFilter = new SearchFilter(getProperties().getUserFindOneFilter());
    searchFilter.setParameter(0, userName);
    final SearchRequest searchRequest = new SearchRequest(
        getProperties().getUserBaseDn(),
        searchFilter);
    searchRequest.setSearchScope(getProperties().getUserFindOneSearchScope());
    return getLdapTemplate().findOne(searchRequest, domainUserLdapMapper);
  }

  @Override
  public boolean exists(@NotNull String userName) {
    return getLdapTemplate()
        .exists(DomainUser.builder().userName(userName).build(), domainUserLdapMapper);
  }

  @Override
  public DomainUser save(@NotNull DomainUser domainUser) {
    if (!exists(domainUser.getUserName())) {
      kinit();
      final List<String> commands = new ArrayList<>();
      sudo(commands);
      commands.add(getProperties().getSambaToolBinary());
      commands.add("user");
      commands.add("create");
      commands.add(domainUser.getUserName());
      if (StringUtils.hasText(domainUser.getPassword())) {
        commands.add("'" + domainUser.getPassword() + "'");
      } else {
        commands.add("--random-password");
      }
      commands.add("--use-username-as-cn");
      auth(commands);

      CommandExecutor.exec(
          commands,
          null,
          getProperties().getSambaToolExecDir(),
          (CommandExecutorResponseValidator) response -> {
            final String err = response.stderrToOneLine();
            if (err.startsWith("ERROR:") && err.contains("check_password_restrictions")) {
              throw ServiceException.badRequest(
                  "msg=[The password does not meet the complexity criteria!] userName=["
                      + domainUser.getUserName() + "]",
                  "check_password_restrictions");
            }
            if (!exists(domainUser.getUserName())) {
              throw ServiceException.internalServerError("msg=[Saving user failed.] userName=["
                  + domainUser.getUserName() + "] "
                  + CommandExecutorResponse.toExceptionMessage(response));
            }
          });
    }
    return getLdapTemplate().save(domainUser, domainUserLdapMapper);
  }

  @Override
  public void savePassword(@NotNull String userName, @NotNull String newPassword) {
    kinit();
    final List<String> commands = new ArrayList<>();
    sudo(commands);
    commands.add(getProperties().getSambaToolBinary());
    commands.add("user");
    commands.add("setpassword");
    commands.add(userName);
    commands.add("--newpassword='" + newPassword + "'"); // works
    auth(commands);
    CommandExecutor.exec(
        commands,
        null,
        getProperties().getSambaToolExecDir(),
        (CommandExecutorResponseValidator) response -> {
          final String err = response.stderrToOneLine();
          if (err.startsWith("ERROR:") && err.contains("check_password_restrictions")) {
            throw ServiceException.badRequest(
                "msg=[The password does not meet the complexity criteria!] userName=["
                    + userName + "]",
                "check_password_restrictions");
          } else if (StringUtils.hasText(err)) {
            throw ServiceException.internalServerError(
                "msg=[Setting new password failed.] userName=[" + userName + "] "
                    + CommandExecutorResponse.toExceptionMessage(response));
          }
        });
  }

  @Override
  public boolean delete(@NotNull String userName) {

    if (exists(userName)) {
      kinit();
      final List<String> commands = new ArrayList<>();
      sudo(commands);
      commands.add(getProperties().getSambaToolBinary());
      commands.add("user");
      commands.add("delete");
      commands.add(userName);
      auth(commands);
      CommandExecutor.exec(
          commands,
          null,
          getProperties().getSambaToolExecDir(),
          (CommandExecutorResponseValidator) response -> {
            if (exists(userName)) {
              throw ServiceException.internalServerError(
                  "msg=[Deleting user failed.] userName=[" + userName + "] "
                      + CommandExecutorResponse.toExceptionMessage(response));
            }
          });
      return true;
    }
    return false;
  }
}
