package org.bremersee.dccon.repository.cli.parser;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import org.bremersee.dccon.repository.cli.CommandExecutorResponse;
import org.bremersee.dccon.repository.cli.CommandExecutorResponseValidator;
import org.bremersee.exception.ServiceException;

abstract class AbstractDnsEntryValidator implements CommandExecutorResponseValidator {

  abstract String getExpectedResponse();

  abstract String getAction();

  abstract String getErrorCode();

  @Override
  public void validate(CommandExecutorResponse response) {
    if (isNull(response.getStdout())
        || !response.getStdout().toLowerCase().contains(getExpectedResponse().toLowerCase())) {

      String error;
      if (nonNull(response.getStdout())) {
        int index = response.getStderr().indexOf('\n');
        if (index > 0) {
          error = ": " + response.getStdout().substring(0, index);
        } else {
          error = ": " + response.getStdout();
        }
      } else {
        error = ".";
      }
      throw ServiceException.internalServerError(
          String.format("%s dns entry failed%s", getAction(), error),
          getErrorCode());
    }
  }
}
