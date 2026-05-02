package chechelpo.frplm.exceptions.types;

import chechelpo.frplm.exceptions.DomainException;
import chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class UnknownField extends DomainException {
    public UnknownField(String message) {
        super(message, Severity.USER, HttpStatus.UNPROCESSABLE_CONTENT);
    }
}
