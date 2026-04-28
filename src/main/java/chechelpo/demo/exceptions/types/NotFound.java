package chechelpo.demo.exceptions.types;

import chechelpo.demo.exceptions.DomainException;
import chechelpo.demo.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class NotFound extends DomainException {
    public NotFound(String message, Severity severity) {
        super(message,severity, HttpStatus.NOT_FOUND);
    }
}
