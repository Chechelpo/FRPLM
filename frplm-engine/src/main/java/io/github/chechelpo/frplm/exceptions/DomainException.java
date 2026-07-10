package io.github.chechelpo.frplm.exceptions;

import org.springframework.http.HttpStatus;

public abstract class DomainException extends Exception {
    public final HttpStatus status;
    public final Severity severity;

    public DomainException(String message, HttpStatus status, Severity severity) {
        super(message);
        this.status = status;
        this.severity = severity;
    }
}
