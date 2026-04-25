package chechelpo.demo.exceptions.types;

import chechelpo.demo.exceptions.DomainException;
import chechelpo.demo.exceptions.Severity;
import org.springframework.http.HttpStatus;

public class Duplicate extends DomainException {
    public Duplicate(String message, Severity severity) {
        super(message, severity, HttpStatus.CONFLICT);
    }
}
