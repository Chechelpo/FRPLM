package chechelpo.frplm.frameworks.entities.fields.format;


import chechelpo.frplm.exceptions.types.InvalidValue;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NumberCoercer extends Coercer<Number> {
    private NumberCoercer(FieldType type) {
        super(type);
    }

    @Contract(value = "_-> new",pure = true)
    public static @NotNull NumberCoercer create(FieldType type) {
        return new NumberCoercer(type);
    }

    @Override
    public @Nullable Number coerce(Object value) {
        return switch (value){
            case null -> null;
            case Short s -> s;
            case Integer i -> i;
            case Long ii -> {
                if (ii < Integer.MIN_VALUE || ii > Integer.MAX_VALUE) {
                    throw new InvalidValue("Cannot coerce to integer value " + ii);
                }
                yield ii;
            }
            case String s -> Integer.parseInt(s);
            default -> throw new InvalidValue("Cannot coerce to integer value " + value);
        };
    }

}
