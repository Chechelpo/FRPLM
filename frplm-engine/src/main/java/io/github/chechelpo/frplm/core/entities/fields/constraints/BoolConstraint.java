package io.github.chechelpo.frplm.core.entities.fields.constraints;

import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldKind;
import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class BoolConstraint extends Constraint<FieldKind.BooleanKind, Boolean> {
    public BoolConstraint(BoolConstraintsBuilder builder) {
        super(builder,FieldType.BOOLEAN);
    }



    @Contract(value = " -> new", pure = true)
    public static @NotNull BoolConstraintsBuilder builder(){
        return new BoolConstraintsBuilder();
    }

    @Contract(pure = true)
    @Override
    protected @NotNull Optional<String> getInvalidValueReason(@NotNull Boolean value) {
        return Optional.empty();
    }

    public static class BoolConstraintsBuilder extends ABSConstraintsBuilder<BoolConstraint, BoolConstraintsBuilder>{
        @Override
        protected BoolConstraintsBuilder self() {
            return this;
        }

        @Override
        public BoolConstraint build() {
            return new BoolConstraint(this);
        }
    }
}
