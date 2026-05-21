package chechelpo.frplm.frameworks.entities.fields.format;

import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class StringCoercer extends Coercer<String> {
    StringCoercer() {
        super(FieldType.STRING);
    }

    @Contract(value="-> new", pure=true)
    public static @NotNull StringCoercer create() {
        return new StringCoercer();
    }

    @Override
    public String coerce(Object value) {
        return (String) value;
    }
}
