package chechelpo.frplm.frameworks.entities.fields.coercers;

import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import chechelpo.frplm.utils.format.Either;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class StringCoercer extends Coercer<String> {
    StringCoercer() {
        super(FieldType.STRING);
    }

    @Contract(value="-> new", pure=true)
    public static @NotNull StringCoercer create() {
        return new StringCoercer();
    }

    @Override
    @Contract(value = "_ -> !null", pure = true)
    public @NotNull Either<CoerceError, @Nullable String> coerce(@Nullable Object value) {
        return switch (value) {
            case null -> Either.right(null);

            case String s -> Either.right(s);

            default -> Either.left(new CoerceError(
                    "Cannot coerce '" + value + "' to STRING"
            ));
        };
    }
}
