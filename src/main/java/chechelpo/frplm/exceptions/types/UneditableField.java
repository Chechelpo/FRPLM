package chechelpo.frplm.exceptions.types;

import chechelpo.frplm.exceptions.DomainException;
import chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class UneditableField extends DomainException {
  public UneditableField(String fieldName, Severity severity) {
    super("Uneditable field: " + fieldName, severity, HttpStatus.FORBIDDEN);
  }
}
