package chechelpo.demo.frameworks.entities.fields.format;

import chechelpo.demo.frameworks.entities.fields.kinds.FieldType;
import org.jetbrains.annotations.NotNull;

public sealed interface Format<T> permits BoolFormat, EnumFormat, FloatFormat, NumberFormat, StringFormat {
    /** Short description of what this field is supposed to be used for */
    String info();
    @NotNull FieldType type();
}
