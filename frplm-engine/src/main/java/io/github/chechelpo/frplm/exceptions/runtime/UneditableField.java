package io.github.chechelpo.frplm.exceptions.runtime;

import io.github.chechelpo.frplm.exceptions.RuntimeDomainException;
import io.github.chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class UneditableField extends RuntimeDomainException {
  public UneditableField(String fieldName, Severity severity) {
    super("Uneditable field: " + fieldName, severity, HttpStatus.FORBIDDEN);
  }
}
