package chechelpo.frplm.frameworks.entities.fields.format;

import chechelpo.frplm.frameworks.entities.fields.kinds.FieldKind;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import com.sun.jdi.FloatType;
import org.jetbrains.annotations.NotNull;

public final class FloatFormat implements Format<FieldKind.FloatKind> {
    private final String info;
    private final FloatType floatType;

    public FloatFormat(String info, FloatType floatType) {
        this.info = info;
        this.floatType = floatType;
    }
    public enum FloatTypes {
        SLIDER,
        INPUT
    }

    public FloatType getFloatType() {
        return floatType;
    }

    @Override
    public String info() {
        return this.info;
    }

    @Override
    public @NotNull FieldType type() {
        return FieldType.FLOAT;
    }
}
