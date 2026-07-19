package io.github.chechelpo.frplm.utils.prolog;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface PrologSourceValidator {

    ValidationResult validate(String source);

    record ValidationResult(
            boolean valid,
            ErrorType errorType,
            String message,
            String offendingSymbol,
            Integer line,
            Integer column
    ) {
        @Contract(" -> new")
        public static @NonNull ValidationResult success() {
            return new ValidationResult(
                    true,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        @Contract("_, _, _, _, _ -> new")
        public static @NonNull ValidationResult error(
                ErrorType type,
                String message,
                String offendingSymbol,
                int line,
                int column
        ) {
            return new ValidationResult(
                    false,
                    type,
                    message,
                    offendingSymbol,
                    line,
                    column
            );
        }
    }

    enum ErrorType {
        BAD_SYNTAX,
        UNKNOWN_TERM
    }
}