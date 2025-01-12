package org.bremersee.dccon.repository.mapper;

import static org.bremersee.ldaptive.LdaptiveEntryMapper.getAttributeValue;
import static org.bremersee.ldaptive.LdaptiveEntryMapper.getAttributeValuesAsList;
import static org.bremersee.ldaptive.LdaptiveEntryMapper.setAttribute;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.model.DomainComputer;
import org.bremersee.dccon.repository.DomainComputerRepositoryConstants;
import org.bremersee.ldaptive.LdaptiveEntryMapper;
import org.ldaptive.AttributeModification;
import org.ldaptive.LdapEntry;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class DomainComputerLdapMapper extends AbstractLdapMapper
    implements LdaptiveEntryMapper<DomainComputer>, DomainComputerRepositoryConstants {

  /**
   * Instantiates a new abstract ldap mapper.
   *
   * @param properties the properties
   */
  public DomainComputerLdapMapper(DomainControllerProperties properties) {
    super(properties);
  }

  @Override
  public String[] getObjectClasses() {
    return new String[0];
  }

  @Override
  public String mapDn(DomainComputer domainComputer) {
    Assert.hasText(domainComputer.getDistinguishedName(), "DN of domain computer is required.");
    return domainComputer.getDistinguishedName();
  }

  @Override
  public DomainComputer map(LdapEntry ldapEntry) {
    if (isEmpty(ldapEntry)) {
      return null;
    }
    DomainComputer domainComputer = new DomainComputer();
    map(ldapEntry, domainComputer);
    return domainComputer;
  }

  @Override
  public void map(LdapEntry ldapEntry, DomainComputer domainComputer) {
    if (isEmpty(ldapEntry)) {
      return;
    }
    mapSamAccount(ldapEntry, domainComputer);

    domainComputer.setName(getAttributeValue(ldapEntry,
        LDAP_NAME, STRING_VALUE_TRANSCODER, null));
    domainComputer.setDnsHostName(getAttributeValue(ldapEntry,
        LDAP_COMPUTER_DNS_HOST_NAME, STRING_VALUE_TRANSCODER, null));
    domainComputer.setNetworkAddresses(getAttributeValuesAsList(ldapEntry,
        LDAP_COMPUTER_NETWORK_ADDRESS, STRING_VALUE_TRANSCODER));
    domainComputer.setOperatingSystem(getAttributeValue(ldapEntry,
        LDAP_COMPUTER_OPERATING_SYSTEM, STRING_VALUE_TRANSCODER, null));
    domainComputer.setOperatingSystemVersion(getAttributeValue(ldapEntry,
        LDAP_COMPUTER_OPERATING_SYSTEM_VERSION, STRING_VALUE_TRANSCODER, null));
    domainComputer.setDescription(getAttributeValue(ldapEntry,
        LDAP_DESCRIPTION, STRING_VALUE_TRANSCODER, null));
    domainComputer.setServicePrincipalNames(getAttributeValuesAsList(ldapEntry,
        LDAP_COMPUTER_SERVICE_PRINCIPAL_NAME, STRING_VALUE_TRANSCODER));
    domainComputer.setCriticalSystemObject(getAttributeValue(ldapEntry,
        LDAP_IS_CRITICAL_SYSTEM_OBJECT, BOOLEAN_VALUE_TRANSCODER, null));
  }

  @Override
  public AttributeModification[] mapAndComputeModifications(DomainComputer source,
      LdapEntry destination) {

    List<AttributeModification> modifications = new ArrayList<>();

    mapSamAccount(source, destination, modifications);

    setAttribute(destination, LDAP_DESCRIPTION, source.getDescription(), false,
        STRING_VALUE_TRANSCODER, modifications);

    return modifications.toArray(new AttributeModification[0]);
  }
}
