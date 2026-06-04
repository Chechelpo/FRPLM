package chechelpo.frplm.core.entities.fields.coercers;

import chechelpo.frplm.core.entities.fields.kinds.FieldType;
import chechelpo.frplm.utils.format.Either;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract sealed class Coercer<P>
        permits BoolCoercer, FloatCoercer, NumberCoercer, StringCoercer {
    protected final FieldType type;

    protected Coercer(FieldType type) {
        this.type = type;
    }

    public record CoerceError(String message) {}

    @Contract(pure = true)
    public abstract @NotNull Either<CoerceError, P> coerce(@Nullable Object value);

}
