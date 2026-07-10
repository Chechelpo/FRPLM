package io.github.chechelpo.frplm.exceptions.runtime;

import io.github.chechelpo.frplm.exceptions.RuntimeDomainException;
import io.github.chechelpo.frplm.exceptions.Severity;
import org.springframework.http.HttpStatus;

public final class EmptyPatch extends RuntimeDomainException {
    public EmptyPatch(String message) {
      super(message, Severity.SYSTEM, HttpStatus.NO_CONTENT);
    }
}
