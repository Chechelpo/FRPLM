package io.github.chechelpo.frplm.exceptions.runtime;

import io.github.chechelpo.frplm.exceptions.RuntimeDomainException;
import io.github.chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class UnexpectedException extends RuntimeDomainException {
    public UnexpectedException(String message, Severity severity) {
        super(message, severity, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
