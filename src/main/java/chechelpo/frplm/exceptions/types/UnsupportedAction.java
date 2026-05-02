package chechelpo.frplm.exceptions.types;

import chechelpo.frplm.exceptions.DomainException;
import chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class UnsupportedAction extends DomainException {
    public UnsupportedAction(String message) {
        super(message, Severity.EXPECTED, HttpStatus.FORBIDDEN);
    }
}
