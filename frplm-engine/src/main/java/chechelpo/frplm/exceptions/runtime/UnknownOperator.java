package chechelpo.frplm.exceptions.runtime;

import chechelpo.frplm.exceptions.RuntimeDomainException;
import chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class UnknownOperator extends RuntimeDomainException {
  public UnknownOperator(String name) {
    super("Unknown operator: " + name, Severity.USER, HttpStatus.UNPROCESSABLE_CONTENT);
  }
}
