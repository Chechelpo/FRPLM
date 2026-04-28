package chechelpo.demo.exceptions.types;

import chechelpo.demo.exceptions.DomainException;
import chechelpo.demo.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class ExpectedField extends DomainException {
    public ExpectedField(String message, Severity severity) {
        super(message,Severity.EXPECTED, HttpStatus.BAD_REQUEST);
    }
}
