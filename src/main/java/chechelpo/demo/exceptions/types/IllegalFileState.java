package chechelpo.demo.exceptions.types;

import chechelpo.demo.exceptions.DomainException;
import chechelpo.demo.exceptions.Severity;
import org.springframework.http.HttpStatus;

/**
 * Signals an illegal file state. Ex.: a file called avatar.png that is not readable.
 */
public final class IllegalFileState extends DomainException {
    public IllegalFileState(String message) {
        super(message, Severity.SYSTEM, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
