package org.bremersee.dccon.repository;

import static java.util.Objects.isNull;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.converter.TreeSearchScopeConverter;
import org.bremersee.dccon.model.DomainComputer;
import org.bremersee.dccon.model.DomainUser;
import org.bremersee.dccon.model.TreeSearchScope;
import org.bremersee.dccon.repository.automock.MockComponent;
import org.bremersee.dccon.repository.automock.ProfileRequired;
import org.bremersee.dccon.repository.cli.CommandExecutorResponse;
import org.bremersee.exception.ServiceException;
import org.bremersee.ldaptive.LdaptiveEntryMapper;
import org.bremersee.ldaptive.LdaptiveTemplate;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
import org.ldaptive.dn.NameValue;
import org.ldaptive.dn.RDn;
import org.ldaptive.filter.AndFilter;
import org.ldaptive.filter.Filter;
import org.ldaptive.filter.OrFilter;
import org.ldaptive.filter.SubstringFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component("domainComputerRepository")
@ProfileRequired("ldap")
@MockComponent(value = DomainUserRepositoryMock.class, methodsOf = DomainComputerRepository.class)
@Slf4j
public class DomainComputerRepositoryImpl extends AbstractDomainComputerRepository
    implements DomainComputerRepository {

  private final LdaptiveEntryMapper<DomainComputer> domainComputerLdapMapper;

  @Getter(AccessLevel.PROTECTED)
  private final DomainRepository domainRepository;

  DomainComputerRepositoryImpl(
      DomainControllerProperties properties,
      ObjectProvider<LdaptiveTemplate> ldapTemplateProvider,
      LdaptiveEntryMapper<DomainComputer> domainComputerLdapMapper,
      DomainRepository domainRepository) {
    super(properties, ldapTemplateProvider.getIfAvailable());
    this.domainComputerLdapMapper = domainComputerLdapMapper;
    this.domainRepository = domainRepository;
  }

  private Filter getFindAllFilter(String query) {
    //noinspection DuplicatedCode
    Filter objectClassFilter = objectClassFilter();
    if (isNull(query) || query.length() <= 2) {
      return objectClassFilter;
    }
    Filter orFilter = new OrFilter(
        new SubstringFilter(LDAP_DESCRIPTION, null, null, query),
        new SubstringFilter(LDAP_SAM_ACCOUNT_NAME, null, null, query),
        new SubstringFilter(LDAP_NAME, null, null, query),
        new SubstringFilter(LDAP_COMPUTER_NETWORK_ADDRESS, null, null, query),
        new SubstringFilter(LDAP_COMPUTER_OPERATING_SYSTEM, null, null, query),
        new SubstringFilter(LDAP_COMPUTER_OPERATING_SYSTEM_VERSION, null, null, query)
    );
    return new AndFilter(objectClassFilter, orFilter);
  }

  @Override
  public Stream<DomainComputer> findAll(String query, Dn ou, TreeSearchScope searchScope) {
    log.debug("findAll({}, {}, {})", query, ou, searchScope);
    SearchScope scope = TreeSearchScopeConverter.toSearchScope(searchScope);
    SearchRequest searchRequest = searchAllRequest(
        ou,
        getFindAllFilter(query),
        scope,
        getReturnAttributes());
    log.debug("findAll, searchRequest = {}", searchRequest);
    return getLdapTemplate()
        .findAll(searchRequest, domainComputerLdapMapper)
        .filter(getIgnoredObjectFilter(ou, scope));
  }

  @Override
  public Optional<DomainComputer> findOne(String name, Dn ou, TreeSearchScope searchScope) {
    log.debug("findOne({})", name);
    String samAccountName;
    if (!name.endsWith("$")) {
      samAccountName = name + "$";
    } else {
      samAccountName = name;
    }
    SearchScope scope = TreeSearchScopeConverter.toSearchScope(searchScope);
    SearchRequest searchRequest = searchOneRequest(samAccountName, ou, scope);
    log.debug("findOne, searchRequest = {}", searchRequest);
    return getLdapTemplate()
        .findOne(searchRequest, domainComputerLdapMapper)
        .filter(getIgnoredObjectFilter(ou, scope));
  }

  @ProfileRequired({"cli", "ldap"})
  @Override
  public DomainComputer update(DomainComputer domainComputer, Dn newOu) {
    log.debug("update({}, {})", domainComputer.getSamAccountName(), newOu);
    DomainComputer existingDomainComputer = findOne(
        domainComputer.getSamAccountName(), null, null)
        .orElseThrow(() -> ServiceException.notFoundWithErrorCode(
            DomainComputer.class.getSimpleName(),
            domainComputer.getSamAccountName(),
            EC_SAM_ACCOUNT_NOT_FOUND));
    Dn oldDn = new Dn(existingDomainComputer.getDistinguishedName());
    Dn newDn = getNewDn(existingDomainComputer, domainComputer, newOu);
    if (!oldDn.isSame(newDn) && getDomainRepository().dnExistsWithAnyObjectClass(newDn.format())) {
      throw ServiceException.alreadyExistsWithErrorCode(
          DomainUser.class.getSimpleName(),
          getProperties().removeBaseDn(newDn),
          EC_DN_ALREADY_EXISTS);
    }
    if (!oldDn.isSame(newDn)) {
      return getLdapTemplate().save(move(domainComputer, newOu), domainComputerLdapMapper);
    }
    return getLdapTemplate().save(domainComputer, domainComputerLdapMapper);
  }

  @ProfileRequired({"cli", "ldap"})
  @Override
  public boolean delete(String name) {
    log.debug("delete({})", name);
    if (findOne(name, null, null).isEmpty()) {
      return false;
    }
    String samAccountName;
    if (name.endsWith("$")) {
      samAccountName = name.substring(0, name.length() - 1);
    } else {
      samAccountName = name;
    }
    List<String> commands = List.of(
        getProperties().getCli().getSambaToolBinary(),
        "computer",
        "delete",
        samAccountName
    );
    return executeAndGet(
        commands,
        response -> findOne(name, null, null).isEmpty());
  }

  DomainComputer move(DomainComputer domainComputer, Dn newOu) {
    String ou = getProperties().removeBaseDn(newOu).format();
    List<String> commands = List.of(
        getProperties().getCli().getSambaToolBinary(),
        "computer",
        "move",
        domainComputer.getSamAccountNameWithoutTrailingDollarSign(),
        quote(ou)
    );
    String newDn = executeAndGet(
        commands,
        response -> getDomainRepository()
            .findDnOfSamAccountName(domainComputer.getSamAccountName())
            .orElseThrow(() -> ServiceException
                .internalServerError(String.format("Moving user '%s' to '%s' failed. %s",
                        domainComputer.getSamAccountName(), ou,
                        CommandExecutorResponse.toExceptionMessage(response)),
                    EC_UPDATING_USER_FAILED)));

    domainComputer.setDistinguishedName(newDn);
    return domainComputer;
  }

  Dn getNewDn(DomainComputer oldDomainComputer, DomainComputer newDomainComputer, Dn newOu) {
    if (isEmpty(newOu) || newOu.isEmpty()) {
      return new Dn(oldDomainComputer.getDistinguishedName());
    }
    String rdnName = new Dn(oldDomainComputer.getDistinguishedName())
        .getRDn().getNameValue().getName();
    String rdnValue = newDomainComputer.getName();
    Dn newDn = new Dn(new RDn(new NameValue(rdnName, rdnValue)));
    newDn.add(getProperties().getBaseDn(validateOu(newOu)));
    return newDn;
  }

}
