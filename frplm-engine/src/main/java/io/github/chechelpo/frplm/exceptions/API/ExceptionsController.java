package io.github.chechelpo.frplm.exceptions.API;

import ch.qos.logback.classic.Logger;
import io.github.chechelpo.frplm.exceptions.DomainException;
import io.github.chechelpo.frplm.exceptions.RuntimeDomainException;
import io.github.chechelpo.frplm.exceptions.Severity;
import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
final class ExceptionsController {
    private static final Logger log = (Logger) LoggerFactory.getLogger("ExceptionController");

    ExceptionsController() {}

    record ErrorResponse(
            int status,
            String type,
            String message,
            String path,
            Severity severity) {}

    @ExceptionHandler(RuntimeDomainException.class)
    @NotNull
    ResponseEntity<ErrorResponse> handleRuntimeDomainException(
            @NotNull RuntimeDomainException e,
            @NotNull HttpServletRequest request
    ) {
        logOnSeverity(request, e);

        ErrorResponse error = new ErrorResponse(
                e.status().value(),
                e.getClass().getSimpleName(),
                e.getMessage(),
                request.getRequestURI(),
                e.severity()
        );

        return ResponseEntity.status(e.status()).body(error);
    }

    @ExceptionHandler(DomainException.class)
    @NotNull
    ResponseEntity<ErrorResponse> handleDomainException(
            @NotNull DomainException e,
            @NotNull HttpServletRequest request
    ) {
        log.error(e.getMessage());

        ErrorResponse error = new ErrorResponse(
                e.status.value(),
                e.getClass().getSimpleName(),
                e.getMessage(),
                request.getRequestURI(),
                e.severity
        );

        return ResponseEntity.status(e.status).body(error);
    }

    @ExceptionHandler(Exception.class)
    @NotNull
    ResponseEntity<ErrorResponse> handleUnhandledException(
            @NotNull Exception e,
            @NotNull HttpServletRequest request
    ) {
        log.error("Unhandled exception on {}", request.getRequestURI(), e);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                e.getClass().getSimpleName(),
                e.getMessage() != null ? e.getMessage() : "Unexpected server error",
                request.getRequestURI(),
                Severity.SYSTEM
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private void logOnSeverity(@NotNull HttpServletRequest request, @NotNull RuntimeDomainException exc){
        StringBuilder builder = new StringBuilder();
        builder.append("Request Failed: ");
        builder.append(request.getRequestURI());

        switch(exc.severity()){
            case EXPECTED -> log.debug(builder.toString(), exc);
            case USER -> log.warn(builder.toString(), exc);
            case SYSTEM -> log.error(builder.toString(), exc);
        }
    }



}
