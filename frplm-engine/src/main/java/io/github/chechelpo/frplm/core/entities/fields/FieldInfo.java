package io.github.chechelpo.frplm.core.entities.fields;

import io.github.chechelpo.frplm.core.entities.fields.coercers.*;
import io.github.chechelpo.frplm.core.entities.fields.constraints.*;
import io.github.chechelpo.frplm.core.entities.fields.coercers.*;
import io.github.chechelpo.frplm.core.entities.fields.constraints.*;
import io.github.chechelpo.frplm.core.entities.fields.coercers.*;
import io.github.chechelpo.frplm.core.entities.fields.constraints.*;
import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldKind;
import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 */
public final class FieldInfo<T extends FieldKind>{
    public final @NotNull FieldType type;
    public final @NotNull Coercer<T> format;
    public final @NotNull Constraint<T, ?> constraints;
    public final boolean require;

    @Contract(pure = true)
    private FieldInfo(@NotNull FieldInfoBuilder<T> builder) {
        this.type = builder.type;
        this.format = builder.format;
        this.require = builder.require;
        this.constraints = builder.constraints;
    }

    @Contract(value = " -> new", pure = true)
    public static @NotNull FieldInfoBuilder<FieldKind.StringKind> stringField() {
        return new FieldInfoBuilder<>(FieldType.STRING);
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull FieldInfoBuilder<FieldKind.NumberKind> numberField(@NotNull FieldType type) {
        if (type.isValidNumber())
            return new FieldInfoBuilder<>(type);
        throw new IllegalArgumentException("Type " + type + " is not supported");
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull FieldInfoBuilder<FieldKind.FloatKind> floatField(@NotNull FieldType type) {
        if (type.isValidFloat())
           return new FieldInfoBuilder<>(type);
        throw new IllegalArgumentException("Type " + type + " is not supported");
    }

    @Contract(value = " -> new", pure = true)
    public static @NotNull FieldInfoBuilder<FieldKind.BooleanKind> booleanField() {
        return new FieldInfoBuilder<>(FieldType.BOOLEAN);
    }

    public static class FieldInfoBuilder<T extends FieldKind> {
        private final FieldType type;
        private Coercer<T> format;
        private Constraint<T, ?> constraints;
        private boolean require;

        private FieldInfoBuilder(FieldType type) {
            this.type = type;
            this.constraints = (Constraint<T, ?>) getDefaultConstraint(type);
            this.format = getDefaultCoercer(type);
        }

        public FieldInfoBuilder<T> setFormat(Coercer format) {
            this.format = format;
            return this;
        }
        public <C extends Constraint<T, ?>> FieldInfoBuilder<T> setConstraints(
                Constraint.@NotNull ABSConstraintsBuilder<C, ?> builder
        ) {
            return setConstraints(builder.build());
        }
        public FieldInfoBuilder<T> setConstraints(Constraint<T, ?> constraints) {
            this.constraints = constraints;
            return this;
        }
        public FieldInfoBuilder<T> require() {
            this.require = true;
            return this;
        }

        public FieldInfo<T> build() {
            return new FieldInfo<T>(this);
        }
    }

    private static <T extends FieldKind> @NotNull Constraint<T, ?> getDefaultConstraint(
            @NotNull FieldType type
    ) {
        return switch (type) {
            case STRING -> (Constraint<T, ?>) StringConstraint.builder()
                    .build();

            case BOOLEAN -> (Constraint<T, ?>) new BoolConstraint(new BoolConstraint.BoolConstraintsBuilder());

            case BYTE, SHORT, INTEGER, LONG -> (Constraint<T, ?>) NumberConstraint.builder(type)
                    .build();

            case FLOAT, DOUBLE -> (Constraint<T, ?>) new FloatConstraint.FloatConstraintsBuilder(type)
                    .build();
        };
    }

    private static @NotNull Coercer getDefaultCoercer(@NotNull FieldType type) {
        return switch (type) {
            case STRING -> StringCoercer.create();
            case BYTE, INTEGER, LONG, SHORT -> NumberCoercer.create(type);
            case FLOAT, DOUBLE -> FloatCoercer.create(type);
            case BOOLEAN -> BoolCoercer.create();
        };
    }
}