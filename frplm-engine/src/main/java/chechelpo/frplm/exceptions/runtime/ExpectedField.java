package chechelpo.frplm.exceptions.runtime;

import chechelpo.frplm.exceptions.RuntimeDomainException;
import chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class ExpectedField extends RuntimeDomainException {
    public ExpectedField(String message, Severity severity) {
        super(message,Severity.EXPECTED, HttpStatus.BAD_REQUEST);
    }
}
