package chechelpo.frplm.core.entities.pseudo_services;

import chechelpo.frplm.core.entities.fields.constraints.Constraint;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.jooq.TableField;
import org.jooq.TableRecord;

import java.util.*;
import java.util.stream.Collectors;

final class DataValidator {
    private DataValidator() {}

    enum VALIDATION_ERROR {
        UNKNOWN_FIELD,
        NON_KEY_FIELD,
        INVALID_VALUE,
        INCOMPLETE_ASSIGNMENTS,
    }

    record FieldValidationError<R extends TableRecord<R>>(
            VALIDATION_ERROR type, TableField<R, ?> fieldName, Optional<Object> value, Optional<String> extraMessage) {
        FieldValidationError {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(extraMessage, "extraMessage");
        }

        public @NotNull String getMessage() {
            return type.toString() + " found: " + fieldName
                    + "\nVALUE: " + value.orElse("unimportant")
                    + "\nMESSAGE: " + extraMessage.orElse("<no extra section>");
        }
    }

    @Contract(pure = true)
    private static <R extends TableRecord<R>> Optional<FieldValidationError<R>> getIfUnknownField(
            TableField<R, ?> field,
            @NotNull @UnmodifiableView Map<TableField<R, ?>, Constraint<?, ?>> constraints
    ) {
        if (!constraints.containsKey(field)) {
            return Optional.of(new FieldValidationError<>(
                    VALIDATION_ERROR.UNKNOWN_FIELD, field, Optional.empty(), Optional.empty()
            ));
        }

        return Optional.empty();
    }

    @Contract(pure = true)
    static <R extends TableRecord<R>> @NotNull Optional<FieldValidationError<R>> getValidationError(
            Map<TableField<R, ?>, Constraint<?, ?>> constraints,
            Set<TableField<R, ?>> requiredFields,
            @NotNull Map<TableField<R, ?>, Object> assignments,
            boolean mustContainAllRequiredFields,
            boolean mustContainOnlyRequiredFields
    ) {
        for (Map.Entry<TableField<R, ?>, Object> entry : assignments.entrySet()) {
            Optional<FieldValidationError<R>> unknownField = getIfUnknownField(entry.getKey(), constraints);
            if (unknownField.isPresent()) return unknownField;

            if (mustContainOnlyRequiredFields && !requiredFields.contains(entry.getKey())) {
                return Optional.of(new FieldValidationError<>(
                        VALIDATION_ERROR.NON_KEY_FIELD, entry.getKey(), Optional.ofNullable(entry.getValue()), Optional.empty())
                );
            }

            Constraint<?, ?> con = constraints.get(entry.getKey());
            Optional<Constraint.ConstraintViolation<R>> constraintViolation = con.validateConstraint(entry.getKey(), entry.getValue(), false);
            if (constraintViolation.isPresent())
                return Optional.of(new FieldValidationError<>(
                        VALIDATION_ERROR.INVALID_VALUE,
                        entry.getKey(), Optional.ofNullable(entry.getValue()),
                        Optional.of(constraintViolation.get().message()))
                );
        }

        if (mustContainAllRequiredFields && !assignments.keySet().containsAll(requiredFields)) {
            Set<TableField<R, ?>> missingFields = new HashSet<>(requiredFields);
            missingFields.removeAll(assignments.keySet());

            return Optional.of(new FieldValidationError<>(
                    VALIDATION_ERROR.INCOMPLETE_ASSIGNMENTS,
                    null,
                    Optional.empty(),
                    Optional.of("Missing required fields: " + formatFieldNames(missingFields))
            ));
        }

        return Optional.empty();
    }

    private static <R extends TableRecord<R>> String formatFieldNames(Set<TableField<R, ?>> fields) {
        return fields.stream()
                .map(TableField::getName)
                .sorted()
                .collect(Collectors.joining(", "));
    }


}
