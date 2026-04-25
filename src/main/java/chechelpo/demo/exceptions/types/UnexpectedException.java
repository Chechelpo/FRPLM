package chechelpo.demo.exceptions.types;

import chechelpo.demo.exceptions.DomainException;
import chechelpo.demo.exceptions.Severity;
import org.springframework.http.HttpStatus;

public class UnexpectedException extends DomainException {
    public UnexpectedException(String message, Severity severity) {
        super(message, severity, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
