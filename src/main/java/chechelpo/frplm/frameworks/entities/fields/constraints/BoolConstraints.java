package chechelpo.frplm.frameworks.entities.fields.constraints;

import chechelpo.frplm.exceptions.types.InvalidValue;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldKind;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BoolConstraints extends Constraints<FieldKind.BooleanKind, Boolean> {
    public BoolConstraints(BoolConstraintsBuilder builder) {
        super(builder,FieldType.BOOLEAN);
    }



    @Contract(value = " -> new", pure = true)
    public static @NotNull BoolConstraintsBuilder builder(){
        return new BoolConstraintsBuilder();
    }

    @Override
    protected void throwIfImpossibleValue(@NotNull Boolean value) throws InvalidValue {}

    public static class BoolConstraintsBuilder extends ABSConstraintsBuilder<BoolConstraints, BoolConstraintsBuilder>{
        @Override
        protected BoolConstraintsBuilder self() {
            return this;
        }

        @Override
        public BoolConstraints build() {
            return new BoolConstraints(this);
        }
    }
}
