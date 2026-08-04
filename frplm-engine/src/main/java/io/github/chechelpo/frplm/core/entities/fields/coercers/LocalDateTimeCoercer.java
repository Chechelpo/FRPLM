package io.github.chechelpo.frplm.core.entities.fields.coercers;

import io.github.chechelpo.frplm.utils.format.Either;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class LocalDateTimeCoercer implements Coercer<LocalDateTime> {

    public static final LocalDateTimeCoercer instance =
            new LocalDateTimeCoercer();

    private LocalDateTimeCoercer() {
    }

    @Override
    public @NotNull Either<CoerceError, LocalDateTime> coerce(
            @Nullable Object value
    ) {
        if (value == null) {
            return Either.left(
                    new CoerceError("Cannot coerce null to LocalDateTime")
            );
        }

        if (value instanceof LocalDateTime localDateTime) {
            return Either.right(localDateTime);
        }

        if (value instanceof Timestamp timestamp) {
            return Either.right(timestamp.toLocalDateTime());
        }

        if (value instanceof OffsetDateTime offsetDateTime) {
            return Either.right(offsetDateTime.toLocalDateTime());
        }

        if (value instanceof ZonedDateTime zonedDateTime) {
            return Either.right(zonedDateTime.toLocalDateTime());
        }

        if (value instanceof LocalDate localDate) {
            return Either.right(localDate.atStartOfDay());
        }

        if (value instanceof CharSequence sequence) {
            return parse(sequence.toString());
        }

        return Either.left(
                new CoerceError(
                        "Cannot coerce value of type "
                                + value.getClass().getTypeName()
                                + " to LocalDateTime"
                )
        );
    }

    private static @NotNull Either<CoerceError, LocalDateTime> parse(
            @NotNull String value
    ) {
        String normalized = value.trim();

        if (normalized.isEmpty()) {
            return Either.left(
                    new CoerceError(
                            "Cannot coerce an empty string to LocalDateTime"
                    )
            );
        }

        LocalDateTime parsed = tryParse(
                normalized,
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        );

        if (parsed != null) {
            return Either.right(parsed);
        }

        parsed = tryParse(
                normalized,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        );

        if (parsed != null) {
            return Either.right(parsed);
        }

        parsed = tryParse(
                normalized,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        );

        if (parsed != null) {
            return Either.right(parsed);
        }

        try {
            return Either.right(
                    OffsetDateTime.parse(normalized).toLocalDateTime()
            );
        } catch (DateTimeParseException ignored) {
            // Continue to the final error.
        }

        try {
            return Either.right(
                    ZonedDateTime.parse(normalized).toLocalDateTime()
            );
        } catch (DateTimeParseException ignored) {
            // Continue to the final error.
        }

        return Either.left(
                new CoerceError(
                        "Cannot parse LocalDateTime from \""
                                + normalized
                                + "\". Expected an ISO-8601 date-time or "
                                + "yyyy-MM-dd HH:mm:ss[.SSS]"
                )
        );
    }

    private static @Nullable LocalDateTime tryParse(
            @NotNull String value,
            @NotNull DateTimeFormatter formatter
    ) {
        try {
            return LocalDateTime.parse(value, formatter);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}