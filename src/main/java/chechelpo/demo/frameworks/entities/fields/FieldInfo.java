package chechelpo.demo.frameworks.entities.fields;

import chechelpo.demo.frameworks.entities.fields.constraints.Constraints;
import chechelpo.demo.frameworks.entities.fields.format.Format;
import chechelpo.demo.frameworks.entities.fields.kinds.FieldKind;
import chechelpo.demo.frameworks.entities.fields.kinds.FieldType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 */
public final class FieldInfo<T extends FieldKind>{
    public final @NotNull FieldType type;
    public final @Nullable Format<T> format;
    public final @Nullable Constraints<T, ?> constraints;
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
            return new FieldInfoBuilder<>(FieldType.INTEGER);
        throw new IllegalArgumentException("Type " + type + " is not supported");
    }

    @Contract(value = " -> new", pure = true)
    public static @NotNull FieldInfoBuilder<FieldKind.FloatKind> floatField(FieldType type) {
        if (!type.isValidFloat())
           return new FieldInfoBuilder<>(FieldType.FLOAT);
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
        }

        public FieldInfoBuilder<T> setFormat(Format<T> format) {
            this.format = format;
            return this;
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

}