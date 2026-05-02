package chechelpo.frplm.exceptions.types;

import chechelpo.frplm.exceptions.DomainException;
import chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class Duplicate extends DomainException {
    public Duplicate(String message, Severity severity) {
        super(message, severity, HttpStatus.CONFLICT);
    }
}
