package org.bremersee.dccon.repository.cli.parser;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bremersee.dccon.ErrorCode;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DnsEntryUpdateValidator extends AbstractDnsEntryValidator {

  private static DnsEntryUpdateValidator instance;

  public static DnsEntryUpdateValidator getInstance() {
    if (instance == null) {
      instance = new DnsEntryUpdateValidator();
    }
    return instance;
  }

  @Override
  String getExpectedResponse() {
    return "Record updated successfully";
  }

  @Override
  String getAction() {
    return "Updating";
  }

  @Override
  String getErrorCode() {
    return ErrorCode.EC_UPDATING_DNS_ENTRY_FAILED;
  }

}
