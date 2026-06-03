package chechelpo.frplm.frameworks.entities.fields.coercers;

import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import chechelpo.frplm.utils.format.Either;
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
    @Contract(value = "_ -> !null", pure = true)
    public @NotNull Either<CoerceError, @Nullable Boolean> coerce(@Nullable Object value) {
        return switch (value) {
            case null -> Either.right(null);

            case Boolean b -> Either.right(b);

            case String s -> switch (s.trim().toLowerCase()) {
                case "true" -> Either.right(true);
                case "false" -> Either.right(false);
                default -> Either.left(new CoerceError("Cannot coerce '" + value + "' to boolean"));
            };

            default -> Either.left(new CoerceError("Cannot coerce '" + value + "' to boolean"));
        };
    }
}
