package chechelpo.frplm.exceptions.types;

import chechelpo.frplm.exceptions.DomainException;
import chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class InvalidKey extends DomainException {
    public InvalidKey(String message, Severity severity) {
        super(message, severity, HttpStatus.BAD_REQUEST);
    }
}
