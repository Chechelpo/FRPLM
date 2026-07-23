package io.github.chechelpo.frplm.exceptions.runtime;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.exceptions.RuntimeDomainException;
import io.github.chechelpo.frplm.exceptions.Severity;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;

public final class EntityNotFound extends RuntimeDomainException {
    public EntityNotFound(@NotNull String message, Severity severity) {
        super(message, severity, HttpStatus.NOT_FOUND);
    }
    public EntityNotFound(EntityReader.RecordFindResult.NotFound<?> notFound, Severity severity){
        super(notFound.toDebugString(), severity, HttpStatus.NOT_FOUND);
    }
}
