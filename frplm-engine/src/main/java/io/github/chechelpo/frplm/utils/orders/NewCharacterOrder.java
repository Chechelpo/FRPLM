package io.github.chechelpo.frplm.utils.orders;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import org.jetbrains.annotations.Nullable;
import org.jooq.TableField;

import java.util.Optional;
import java.util.Set;

public record NewCharacterOrder(
        EntityDataPayload<CharactersRecord> payload,
        NewLorebookOrder lorebook
) implements CreationOrder<CharactersRecord> {
    @Override
    public Optional<String> getZipPath() {
        return Optional.empty();
    }

}
