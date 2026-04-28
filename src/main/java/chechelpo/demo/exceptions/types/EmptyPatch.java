package chechelpo.demo.exceptions.types;

import chechelpo.demo.exceptions.DomainException;
import chechelpo.demo.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class EmptyPatch extends DomainException {
    public EmptyPatch(String message) {
      super(message, Severity.SYSTEM, HttpStatus.NO_CONTENT);
    }
}
