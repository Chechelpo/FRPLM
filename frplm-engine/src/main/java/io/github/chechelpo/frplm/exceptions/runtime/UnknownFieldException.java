package io.github.chechelpo.frplm.exceptions.runtime;

import io.github.chechelpo.frplm.exceptions.RuntimeDomainException;
import io.github.chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class UnknownFieldException extends RuntimeDomainException {
    public UnknownFieldException(String message) {
        super(message, Severity.USER, HttpStatus.UNPROCESSABLE_CONTENT);
    }
    public UnknownFieldException(String message, Severity severity) {
        super(message, severity, HttpStatus.UNPROCESSABLE_CONTENT);
    }
}
