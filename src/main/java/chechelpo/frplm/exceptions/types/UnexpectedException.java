package chechelpo.frplm.exceptions.types;

import chechelpo.frplm.exceptions.DomainException;
import chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class UnexpectedException extends DomainException {
    public UnexpectedException(String message, Severity severity) {
        super(message, severity, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
