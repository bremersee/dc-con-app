package org.bremersee.dccon.repository;

import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.ldaptive.LdaptiveTemplate;
import org.ldaptive.dn.Dn;

abstract class AbstractDomainComputerRepository extends AbstractSamAccountRepository
    implements DomainComputerRepositoryConstants {

  /**
   * Instantiates a new abstract repository.
   *
   * @param properties the properties
   * @param ldapTemplate the ldap template
   */
  AbstractDomainComputerRepository(
      DomainControllerProperties properties,
      LdaptiveTemplate ldapTemplate) {
    super(properties, ldapTemplate);
  }

  @Override
  Dn getDefaultOu() {
    return getProperties().getComputer().getDefaultOu();
  }

  @Override
  String getObjectClassValue() {
    return LDAP_OBJECT_CLASS_COMPUTER;
  }

  @Override
  String[] getBinaryAttributes() {
    return LDAP_COMPUTER_BINARY_ATTRIBUTES;
  }

  @Override
  String[] getReturnAttributes() {
    return LDAP_COMPUTER_MAPPED_ATTRIBUTES;
  }
}
