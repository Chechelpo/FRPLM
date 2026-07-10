package io.github.chechelpo.frplm.exceptions.runtime;

import io.github.chechelpo.frplm.exceptions.RuntimeDomainException;
import io.github.chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class UnknownOperator extends RuntimeDomainException {
  public UnknownOperator(String name) {
    super("Unknown operator: " + name, Severity.USER, HttpStatus.UNPROCESSABLE_CONTENT);
  }
}
