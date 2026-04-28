package chechelpo.demo.exceptions.types;

import chechelpo.demo.exceptions.DomainException;
import chechelpo.demo.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class UnknownAsset extends DomainException {
    public UnknownAsset(String message) {
        super(message, Severity.USER, HttpStatus.UNPROCESSABLE_CONTENT);
    }
}
