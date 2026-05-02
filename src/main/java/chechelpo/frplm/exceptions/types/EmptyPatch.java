package chechelpo.frplm.exceptions.types;

import chechelpo.frplm.exceptions.DomainException;
import chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class EmptyPatch extends DomainException {
    public EmptyPatch(String message) {
      super(message, Severity.SYSTEM, HttpStatus.NO_CONTENT);
    }
}
