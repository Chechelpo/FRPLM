package chechelpo.frplm.exceptions.runtime;

import chechelpo.frplm.exceptions.RuntimeDomainException;
import chechelpo.frplm.exceptions.Severity;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;

public final class EntityNotFound extends RuntimeDomainException {
    public EntityNotFound(@NotNull String message, Severity severity) {
        super(message, severity, HttpStatus.NOT_FOUND);
    }
}
