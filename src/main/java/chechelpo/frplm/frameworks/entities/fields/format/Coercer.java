package chechelpo.frplm.frameworks.entities.fields.format;

import chechelpo.frplm.exceptions.types.InvalidValue;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public abstract sealed class Coercer<P>
        permits BoolCoercer, FloatCoercer, NumberCoercer, StringCoercer {
    protected final FieldType type;

    protected Coercer(FieldType type) {
        this.type = type;
    }

    @Contract(value = "null -> null ; !null -> !null", pure = true)
    public abstract @Nullable P coerce(@Nullable Object value) throws InvalidValue;

}
