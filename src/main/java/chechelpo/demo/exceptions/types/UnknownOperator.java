package chechelpo.demo.exceptions.types;

import chechelpo.demo.exceptions.DomainException;
import chechelpo.demo.exceptions.Severity;
import org.springframework.http.HttpStatus;

public class UnknownOperator extends DomainException {
  public UnknownOperator(String name) {
    super("Unknown operator: " + name, Severity.USER, HttpStatus.UNPROCESSABLE_CONTENT);
  }
}
