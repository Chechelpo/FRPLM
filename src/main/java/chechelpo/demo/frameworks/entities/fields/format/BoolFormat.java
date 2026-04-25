package chechelpo.demo.frameworks.entities.fields.format;

import chechelpo.demo.frameworks.entities.fields.kinds.FieldKind;
import chechelpo.demo.frameworks.entities.fields.kinds.FieldType;
import org.jetbrains.annotations.NotNull;

public record BoolFormat(String info) implements Format<FieldKind.BooleanKind> {
    private static final String FORMAT = "Bool";

    @Override
    public @NotNull FieldType type() {
        return FieldType.BOOLEAN;
    }
}
