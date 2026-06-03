package chechelpo.frplm.exceptions.runtime;

import chechelpo.frplm.exceptions.RuntimeDomainException;
import chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class InvalidKey extends RuntimeDomainException {
    public InvalidKey(String message, Severity severity) {
        super(message, severity, HttpStatus.BAD_REQUEST);
    }
}
