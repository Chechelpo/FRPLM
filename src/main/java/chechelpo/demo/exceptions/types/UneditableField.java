package chechelpo.demo.exceptions.types;

import chechelpo.demo.exceptions.DomainException;
import chechelpo.demo.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class UneditableField extends DomainException {
  public UneditableField(String fieldName, Severity severity) {
    super("Uneditable field: " + fieldName, severity, HttpStatus.FORBIDDEN);
  }
}
