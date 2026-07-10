package io.github.chechelpo.frplm.exceptions.runtime;

import io.github.chechelpo.frplm.exceptions.RuntimeDomainException;
import io.github.chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class UnsupportedAction extends RuntimeDomainException {
    public UnsupportedAction(String message) {
        super(message, Severity.EXPECTED, HttpStatus.FORBIDDEN);
    }
    public UnsupportedAction(String message, Severity severity) {
        super(message, severity, HttpStatus.FORBIDDEN);
    }
}
