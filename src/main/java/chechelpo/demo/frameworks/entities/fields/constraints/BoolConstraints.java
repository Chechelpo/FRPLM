package chechelpo.demo.frameworks.entities.fields.constraints;

import chechelpo.demo.frameworks.entities.fields.kinds.FieldKind;
import chechelpo.demo.frameworks.entities.fields.kinds.FieldType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BoolConstraints extends Constraints<FieldKind.BooleanKind, Boolean> {
    public BoolConstraints(boolean read_only) {
        super(read_only, FieldType.BOOLEAN);
    }

    @Override
    public @Nullable Boolean coerce(Object value) {
        return switch (value) {
            case null -> null;
            case Boolean b -> b;
            case String s -> Boolean.parseBoolean(s);
            default -> throw new IllegalArgumentException("Cannot coerce '" + value + "' to boolean");
        };
    }
}
