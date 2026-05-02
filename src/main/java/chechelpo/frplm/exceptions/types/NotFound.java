package chechelpo.frplm.exceptions.types;

import chechelpo.frplm.exceptions.DomainException;
import chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class NotFound extends DomainException {
    public NotFound(String message, Severity severity) {
        super(message,severity, HttpStatus.NOT_FOUND);
    }
}
