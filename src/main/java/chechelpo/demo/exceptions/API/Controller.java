package chechelpo.demo.exceptions.API;

import ch.qos.logback.classic.Logger;
import chechelpo.demo.config.logging.LoggerFactory;
import chechelpo.demo.config.logging.Logger_names;
import chechelpo.demo.exceptions.DomainException;
import chechelpo.demo.exceptions.Severity;
import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

@RestController
final class Controller {
    private final Logger log;

    Controller() {
        this.log = LoggerFactory.get_logger(Logger_names.EXCEPTIONS);
    }

    private record ErrorResponse(
            int status,
            String type,
            String message,
            String path,
            Severity severity) {}

    @ExceptionHandler(DomainException.class)
    @NotNull
    ResponseEntity<ErrorResponse> handle_domain_exception(
            @NotNull DomainException e,
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

    private void logOnSeverity(@NotNull HttpServletRequest request, @NotNull DomainException exc){
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
