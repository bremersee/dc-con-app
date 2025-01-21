package org.bremersee.dccon.repository.cli.parser;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bremersee.dccon.ErrorCode;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DnsEntryDeleteValidator extends AbstractDnsEntryValidator {

  private static DnsEntryDeleteValidator instance;

  public static DnsEntryDeleteValidator getInstance() {
    if (instance == null) {
      instance = new DnsEntryDeleteValidator();
    }
    return instance;
  }

  @Override
  String getExpectedResponse() {
    return "Record deleted successfully";
  }

  @Override
  String getAction() {
    return "Deleting";
  }

  @Override
  String getErrorCode() {
    return ErrorCode.EC_DELETING_DNS_ENTRY_FAILED;
  }

}
