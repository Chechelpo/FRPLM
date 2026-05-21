package chechelpo.frplm.frameworks.entities.fields.format;

import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FloatCoercer extends Coercer<Double> {
    private FloatCoercer(FieldType type) {
        super(type);
    }

    @Contract("_ -> new")
    public static @NotNull FloatCoercer create(FieldType type) {
        return new FloatCoercer(type);
    }

    @Override
    public @Nullable Double coerce(Object value) {
        return switch (value) {
            case null -> null;
            case Float f -> f.doubleValue();
            case Double d -> d;
            case Number n -> n.doubleValue();
            case String s -> this.type == FieldType.FLOAT
                    ? Float.parseFloat(s)
                    : Double.parseDouble(s);
            default -> throw new IllegalArgumentException("Cannot coerce " + value + " to " + this.type);
        };
    }
}
