package chechelpo.demo.frameworks.entities.fields.format;

import chechelpo.demo.frameworks.entities.fields.kinds.FieldKind;
import chechelpo.demo.frameworks.entities.fields.kinds.FieldType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class EnumFormat implements Format<FieldKind.EnumKind> {
    private final String info;
    private EnumFormat(String info){
        this.info = info;
    }

    @Contract(pure = true)
    @Override
    public @NotNull String info() {
        return this.info;
    }

    @Override
    public @NotNull FieldType type() {
        return FieldType.ENUM;
    }
}
