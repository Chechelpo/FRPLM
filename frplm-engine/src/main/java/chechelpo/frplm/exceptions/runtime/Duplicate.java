package chechelpo.frplm.exceptions.runtime;

import chechelpo.frplm.exceptions.RuntimeDomainException;
import chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class Duplicate extends RuntimeDomainException {
    public Duplicate(String message, Severity severity) {
        super(message, severity, HttpStatus.CONFLICT);
    }
}
