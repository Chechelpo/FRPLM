package chechelpo.frplm.exceptions.runtime;

import chechelpo.frplm.exceptions.RuntimeDomainException;
import chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

/**
 * Signals an illegal file state. Ex.: a file called avatar.png that is not readable.
 */
public final class IllegalFileState extends RuntimeDomainException {
    public IllegalFileState(String message) {
        super(message, Severity.SYSTEM, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
