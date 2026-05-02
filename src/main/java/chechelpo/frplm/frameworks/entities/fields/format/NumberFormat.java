package chechelpo.frplm.frameworks.entities.fields.format;


import chechelpo.frplm.frameworks.entities.fields.kinds.FieldKind;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class NumberFormat implements Format<FieldKind.NumberKind> {
    private final Types type;
    private final String info;

    @Contract(pure = true)
    private NumberFormat(@NotNull NumberFormatBuilder builder) {
        this.type = builder.type;
        this.info = builder.info;
    }

    @Contract(value = " -> new", pure = true)
    public static @NotNull NumberFormatBuilder builder(){
        return new NumberFormatBuilder();
    }
    public static class NumberFormatBuilder {
        private Types type;
        private String info;

        public NumberFormatBuilder setType(Types type) {
            this.type = type;
            return this;
        }
        public NumberFormatBuilder setInfo(String info) {
            this.info = info;
            return this;
        }

        public NumberFormat build() {
            return new NumberFormat(this);
        }
    }

    public enum Types {
        SLIDER,
        INPUT,
        ;
    }

    @Override
    public String info() {
        return "";
    }

    @Override
    public @NotNull FieldType type() {
        return FieldType.INTEGER;
    }

    public Types getType() {
        return type;
    }

    public String getInfo() {
        return info;
    }
}
