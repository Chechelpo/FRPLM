package chechelpo.frplm.exceptions;

import org.springframework.http.HttpStatus;

public abstract class DomainException extends RuntimeException {

    private final Severity severity;
    private final HttpStatus status;

    protected DomainException(
            String message,
            Severity severity,
            HttpStatus status
    ) {
        super(message);
        this.severity = severity;
        this.status = status;
    }

    public Severity severity() {
        return severity;
    }

    public HttpStatus status() {
        return status;
    }
}
