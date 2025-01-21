package org.bremersee.dccon.repository.cli.parser;

import org.bremersee.dccon.ErrorCode;

public class DnsZoneCreateValidator extends AbstractDnsEntryValidator {

  private final String zoneName;

  public DnsZoneCreateValidator(String zoneName) {
    this.zoneName = zoneName;
  }

  @Override
  String getExpectedResponse() {
    return String.format("Zone %s created successfully", zoneName);
  }

  @Override
  String getAction() {
    return "Creating";
  }

  @Override
  String getErrorCode() {
    return ErrorCode.EC_CREATING_DNS_ZONE_FAILED;
  }

}
