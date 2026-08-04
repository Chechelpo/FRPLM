package io.github.chechelpo.frplm.core.entities.fields.constraints;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

public final class BoolConstraint implements Constraint<Boolean> {
    static final BoolConstraint instance = new BoolConstraint();
    private BoolConstraint(){}

    @Override
    public Optional<String> returnReasonIfInvalid(@NonNull Boolean value) {
        return Optional.empty();
    }
}
