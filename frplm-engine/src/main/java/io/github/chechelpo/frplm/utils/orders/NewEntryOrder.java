package io.github.chechelpo.frplm.utils.orders;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.TableField;

import java.util.Optional;
import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.ENTRY;

public record NewEntryOrder (
        Set<String> keywords,
        EntityDataPayload<EntryRecord> payload
) implements CreationOrder<EntryRecord> {
    @Override
    public @NotNull String toString() {
        return """
                
                Entry %s
                Keywords: %s
                Info: %s
                
                """.formatted(
                        payload.getAssignment(ENTRY.NAME).orElse("No name"),
                        keywords,
                        payload
        );
    }

    @Override
    public Optional<String> getZipPath() {
        return Optional.empty();
    }
}
