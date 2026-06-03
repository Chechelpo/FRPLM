package chechelpo.frplm.exceptions.runtime;

import chechelpo.frplm.exceptions.RuntimeDomainException;
import chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class UnexpectedException extends RuntimeDomainException {
    public UnexpectedException(String message, Severity severity) {
        super(message, severity, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
