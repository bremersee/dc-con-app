package org.bremersee.dccon.repository.mapper;

import org.bremersee.dccon.model.DomainGroup;
import org.bremersee.dccon.model.DomainGroupMember;
import org.bremersee.dccon.repository.DomainGroupRepositoryConstants;
import org.bremersee.dccon.repository.DomainUserRepositoryConstants;
import org.bremersee.ldaptive.LdaptiveEntryMapper;
import org.ldaptive.LdapEntry;

public interface DomainGroupLdapMapper extends LdaptiveEntryMapper<DomainGroup>,
    SamAccountLdapMapper, DomainGroupRepositoryConstants {

  String[] SAM_ACCOUNT_ATTRIBUTES = new String[]{
      LDAP_WHEN_CREATED,
      LDAP_WHEN_CHANGED,
      LDAP_SAM_ACCOUNT_NAME,
      LDAP_OBJECT_SID,
      LDAP_PRIMARY_GROUP_ID,
      LDAP_MEMBER_OF_GROUP
  };

  String[] DOMAIN_GROUP_MEMBER_ATTRIBUTES = new String[]{
      LDAP_WHEN_CREATED,
      LDAP_WHEN_CHANGED,
      LDAP_SAM_ACCOUNT_NAME,
      LDAP_OBJECT_SID,
      LDAP_PRIMARY_GROUP_ID,
      LDAP_OBJECT_CLASS,
      DomainUserRepositoryConstants.LDAP_USER_GIVEN_NAME,
      DomainUserRepositoryConstants.LDAP_USER_SN,
      DomainUserRepositoryConstants.LDAP_USER_DISPLAY_NAME,
      LDAP_NAME
  };

  DomainGroupMember mapDomainGroupMember(LdapEntry entry, boolean selected);

}
