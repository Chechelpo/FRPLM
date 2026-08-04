package io.github.chechelpo.frplm.exceptions.runtime;

import io.github.chechelpo.frplm.exceptions.RuntimeDomainException;
import io.github.chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class InvalidDTO extends RuntimeDomainException {
    public InvalidDTO(String message) {
        super(message, Severity.USER, HttpStatus.BAD_REQUEST);
    }
}
