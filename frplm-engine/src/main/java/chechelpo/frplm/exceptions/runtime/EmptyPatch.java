package chechelpo.frplm.exceptions.runtime;

import chechelpo.frplm.exceptions.RuntimeDomainException;
import chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class EmptyPatch extends RuntimeDomainException {
    public EmptyPatch(String message) {
      super(message, Severity.SYSTEM, HttpStatus.NO_CONTENT);
    }
}
