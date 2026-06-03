package chechelpo.frplm.exceptions.runtime;

import chechelpo.frplm.exceptions.RuntimeDomainException;
import chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class UneditableEntity extends RuntimeDomainException {
    public UneditableEntity(String message) {
        super(message, Severity.USER, HttpStatus.FORBIDDEN);
    }
}
