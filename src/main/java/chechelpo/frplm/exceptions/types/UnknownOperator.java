package chechelpo.frplm.exceptions.types;

import chechelpo.frplm.exceptions.DomainException;
import chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class UnknownOperator extends DomainException {
  public UnknownOperator(String name) {
    super("Unknown operator: " + name, Severity.USER, HttpStatus.UNPROCESSABLE_CONTENT);
  }
}
