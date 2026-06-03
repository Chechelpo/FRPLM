package chechelpo.frplm.exceptions.runtime;

import chechelpo.frplm.exceptions.RuntimeDomainException;
import chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class UnknownAsset extends RuntimeDomainException {
    public UnknownAsset(String message) {
        super(message, Severity.USER, HttpStatus.UNPROCESSABLE_CONTENT);
    }
}
