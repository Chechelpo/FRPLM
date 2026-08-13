package io.github.chechelpo.frplm.core.entities.fields.constraints;

import org.jooq.DataType;
import org.jooq.TableField;
import org.jspecify.annotations.NonNull;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class DefaultConstraints {

    private DefaultConstraints() {
    }

    private static final Map<Class<?>, Constraint<?>> DEFAULT_CONSTRAINTS;

    static {
        Map<Class<?>, Constraint<?>> constraints = new HashMap<>();

        register(byte.class, ByteConstraint.builder().build(), constraints);
        register(Byte.class, ByteConstraint.builder().build(), constraints);

        register(byte[].class, ByteArrayConstraint.builder().build(), constraints);

        register(short.class, ShortConstraint.builder().build(), constraints);
        register(Short.class, ShortConstraint.builder().build(), constraints);

        register(int.class, IntegerConstraint.builder().build(), constraints);
        register(Integer.class, IntegerConstraint.builder().build(), constraints);

        register(long.class, LongConstraint.builder().build(), constraints);
        register(Long.class, LongConstraint.builder().build(), constraints);

        register(float.class, FloatConstraint.builder().build(), constraints);
        register(Float.class, FloatConstraint.builder().build(), constraints);

        register(double.class, DoubleConstraint.builder().build(), constraints);
        register(Double.class, DoubleConstraint.builder().build(), constraints);

        register(boolean.class, BoolConstraint.instance, constraints);
        register(Boolean.class, BoolConstraint.instance, constraints);

        register(String.class, StringConstraint.builder().build(), constraints);

        register(
                LocalDateTime.class,
                LocalDateTimeConstraint.builder().build(),
                constraints
        );

        DEFAULT_CONSTRAINTS = Map.copyOf(constraints);
    }

    private static <T> void register(
            Class<T> clazz,
            Constraint<T> constraint,
            Map<Class<?>, Constraint<?>> constraints
    ) {
        Objects.requireNonNull(clazz, "clazz");
        Objects.requireNonNull(constraint, "constraint");

        Constraint<?> previous = constraints.putIfAbsent(clazz, constraint);

        if (previous != null) {
            throw new IllegalStateException(
                    "A constraint is already registered for " + clazz.getName()
            );
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> @NonNull Constraint<T> getDefaultConstraintForClass(
            TableField<?, T> field
    ) {
        Objects.requireNonNull(field, "field is null");

        DataType<T> dataType = field.getDataType();
        Class<T> clazz = dataType.getType();

        if (clazz == String.class && dataType.lengthDefined()) {
            return (Constraint<T>) StringConstraint.builder()
                    .setMaxLength(dataType.length())
                    .build();
        }

        if (clazz == byte[].class && dataType.lengthDefined()) {
            return (Constraint<T>) ByteArrayConstraint.builder()
                    .maxLength(dataType.length())
                    .build();
        }

        Constraint<?> constraint = DEFAULT_CONSTRAINTS.get(clazz);

        if (constraint == null) {
            throw new IllegalArgumentException(
                    "No default constraint registered for " + clazz.getName()
            );
        }

        return (Constraint<T>) constraint;
    }
}