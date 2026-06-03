package chechelpo.frplm.exceptions.runtime;

import chechelpo.frplm.exceptions.RuntimeDomainException;
import chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class UneditableField extends RuntimeDomainException {
  public UneditableField(String fieldName, Severity severity) {
    super("Uneditable field: " + fieldName, severity, HttpStatus.FORBIDDEN);
  }
}
