package chechelpo.frplm.exceptions.runtime;

import chechelpo.frplm.exceptions.RuntimeDomainException;
import chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public class NotInitialized extends RuntimeDomainException {
    public NotInitialized(String message, Severity severity) {
        super(message, severity, HttpStatus.EXPECTATION_FAILED);
    }
}
