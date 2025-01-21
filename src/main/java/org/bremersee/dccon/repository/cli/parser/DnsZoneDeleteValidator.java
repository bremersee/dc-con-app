package org.bremersee.dccon.repository.cli.parser;

import org.bremersee.dccon.ErrorCode;

public class DnsZoneDeleteValidator extends AbstractDnsEntryValidator {

  private final String zoneName;

  public DnsZoneDeleteValidator(String zoneName) {
    this.zoneName = zoneName;
  }

  @Override
  String getExpectedResponse() {
    return String.format("Zone %s deleted successfully", zoneName);
  }

  @Override
  String getAction() {
    return "Deleting";
  }

  @Override
  String getErrorCode() {
    return ErrorCode.EC_DELETING_DNS_ZONE_FAILED;
  }

}
