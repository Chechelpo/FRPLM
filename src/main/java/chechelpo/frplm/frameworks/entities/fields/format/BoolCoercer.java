package chechelpo.frplm.frameworks.entities.fields.format;

import chechelpo.frplm.exceptions.types.InvalidValue;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BoolCoercer extends Coercer<Boolean> {
    private BoolCoercer(FieldType type) {
        super(type);
    }

    @Contract(" -> new")
    public static @NotNull BoolCoercer create() {
        return new BoolCoercer(FieldType.BOOLEAN);
    }

    @Override
    public @Nullable Boolean coerce(@Nullable Object value) throws InvalidValue {
        return switch (value) {
            case null -> null;
            case Boolean b -> b;
            case String s -> Boolean.parseBoolean(s);
            default -> throw new InvalidValue("Cannot coerce '" + value + "' to boolean");

        };
    }
}
