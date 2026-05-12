package chechelpo.frplm.frameworks.entities.fields;

import chechelpo.frplm.frameworks.entities.fields.constraints.*;
import chechelpo.frplm.frameworks.entities.fields.format.Format;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldKind;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 */
public final class FieldInfo<T extends FieldKind>{
    public final @NotNull FieldType type;
    public final @Nullable Format<T> format;
    public final @NotNull Constraints<T, ?> constraints;
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

    @Contract(value = " -> new", pure = true)
    public static @NotNull FieldInfoBuilder<FieldKind.NumberKind> numberField(FieldType type) {
        if (type.isValidNumber())
            return new FieldInfoBuilder<>(type);
        throw new IllegalArgumentException("Type " + type + " is not supported");
    }

    @Contract(value = " -> new", pure = true)
    public static @NotNull FieldInfoBuilder<FieldKind.FloatKind> floatField(FieldType type) {
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
        private Format<T> format;
        private Constraints<T, ?> constraints;
        private boolean require;

        private FieldInfoBuilder(FieldType type) {
            this.type = type;
            this.constraints = (Constraints<T, ?>) getDefaultConstraint(type);
        }

        public FieldInfoBuilder<T> setFormat(Format<T> format) {
            this.format = format;
            return this;
        }
        public <C extends Constraints<T, ?>> FieldInfoBuilder<T> setConstraints(
                Constraints.@NotNull ABSConstraintsBuilder<C, ?> builder
        ) {
            return setConstraints(builder.build());
        }
        public FieldInfoBuilder<T> setConstraints(Constraints<T, ?> constraints) {
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

    @SuppressWarnings("unchecked")
    private static <T extends FieldKind> @NotNull Constraints<T, ?> getDefaultConstraint(
            @NotNull FieldType type
    ) {
        return switch (type) {
            case STRING -> (Constraints<T, ?>) StringConstraints.builder()
                    .build();

            case BOOLEAN -> (Constraints<T, ?>) new BoolConstraints(new BoolConstraints.BoolConstraintsBuilder());

            case BYTE, SHORT, INTEGER, LONG -> (Constraints<T, ?>) NumberConstraints.builder(type)
                    .build();

            case FLOAT, DOUBLE -> (Constraints<T, ?>) new FloatConstraints.FloatConstraintsBuilder(type)
                    .build();

            case ENUM -> throw new UnsupportedOperationException(
                    "Default enum constraints are not implemented yet"
            );
        };
    }
}