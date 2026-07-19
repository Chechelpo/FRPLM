package io.github.chechelpo.frplm.exceptions.runtime;

import io.github.chechelpo.frplm.exceptions.RuntimeDomainException;
import io.github.chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class InvalidValue extends RuntimeDomainException {
    public InvalidValue(String message) {
        super(message, Severity.EXPECTED, HttpStatus.FORBIDDEN);
    }
    public InvalidValue(String message, Severity severity) {
        super(message, severity, HttpStatus.FORBIDDEN);
    }
}
