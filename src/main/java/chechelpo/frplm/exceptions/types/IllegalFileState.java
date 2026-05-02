package chechelpo.frplm.exceptions.types;

import chechelpo.frplm.exceptions.DomainException;
import chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

/**
 * Signals an illegal file state. Ex.: a file called avatar.png that is not readable.
 */
public final class IllegalFileState extends DomainException {
    public IllegalFileState(String message) {
        super(message, Severity.SYSTEM, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
