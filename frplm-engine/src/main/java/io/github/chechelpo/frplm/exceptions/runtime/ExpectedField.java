package io.github.chechelpo.frplm.exceptions.runtime;

import io.github.chechelpo.frplm.exceptions.RuntimeDomainException;
import io.github.chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class ExpectedField extends RuntimeDomainException {
    public ExpectedField(String message, Severity severity) {
        super(message,Severity.EXPECTED, HttpStatus.BAD_REQUEST);
    }
}
