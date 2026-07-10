package io.github.chechelpo.frplm.exceptions.runtime;

import io.github.chechelpo.frplm.exceptions.RuntimeDomainException;
import io.github.chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class InvalidKey extends RuntimeDomainException {
    public InvalidKey(String message, Severity severity) {
        super(message, severity, HttpStatus.BAD_REQUEST);
    }
}
