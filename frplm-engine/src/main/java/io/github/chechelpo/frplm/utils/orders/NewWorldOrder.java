package io.github.chechelpo.frplm.utils.orders;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jooq.TableField;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * They are turning the frogs gay
 */
public record NewWorldOrder(
        String zipPath,
        EntityDataPayload<WorldsRecord> payload,
        NewLorebookOrder lorebook,
        List<NewLocationOrder> locations,
        List<NewRegionOrder> regions,
        List<NewEdgeOrder> locationEdges
) implements CreationOrder<WorldsRecord> {

    @Contract(pure = true)
    @Override
    public @NonNull Optional<String> getZipPath() {
        return Optional.ofNullable(zipPath);
    }
}
