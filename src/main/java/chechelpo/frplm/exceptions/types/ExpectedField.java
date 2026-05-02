package chechelpo.frplm.exceptions.types;

import chechelpo.frplm.exceptions.DomainException;
import chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class ExpectedField extends DomainException {
    public ExpectedField(String message, Severity severity) {
        super(message,Severity.EXPECTED, HttpStatus.BAD_REQUEST);
    }
}
