package chechelpo.demo.exceptions.types;

import chechelpo.demo.exceptions.DomainException;
import chechelpo.demo.exceptions.Severity;
import org.springframework.http.HttpStatus;

public class InvalidID extends DomainException {
    public InvalidID(String message, Severity severity) {
        super(message, severity, HttpStatus.BAD_REQUEST);
    }
}
