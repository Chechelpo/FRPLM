package chechelpo.demo.frameworks.entities.fields.format;


import chechelpo.demo.frameworks.entities.fields.kinds.FieldKind;
import chechelpo.demo.frameworks.entities.fields.kinds.FieldType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class StringFormat implements Format<FieldKind.StringKind> {
    private final Types type;
    private final String info;

    @Contract(pure = true)
    private StringFormat(@NotNull StringFormatBuilder builder) {
        this.type = builder.type;
        this.info = builder.info;
    }

    public static @NotNull StringFormatBuilder builder() {
        return new StringFormatBuilder();
    }

    public static class StringFormatBuilder {
        private Types type;
        private String info;

        public StringFormatBuilder setType(Types type) {
            this.type = type;
            return this;
        }
        public StringFormatBuilder setInfo(String info) {
            this.info = info;
            return this;
        }

        public StringFormat build() {
            return new StringFormat(this);
        }
    }

    public enum Types {
        SHORT_TEXT,
        LONG_TEXT,
        ;
    }

    @Override
    public String info() {
        return "";
    }

    @Override
    public @NotNull FieldType type() {
        return FieldType.STRING;
    }

    public Types getType() {
        return type;
    }

    public String getInfo() {
        return info;
    }

}
