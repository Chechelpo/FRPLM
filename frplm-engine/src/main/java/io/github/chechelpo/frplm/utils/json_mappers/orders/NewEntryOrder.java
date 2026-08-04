package io.github.chechelpo.frplm.utils.json_mappers.orders;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.ENTRY;

public record NewEntryOrder(Set<String> keywords, EntityDataPayload<EntryRecord> entryInfo){
    @Override
    public @NotNull String toString() {
        return """
                
                Entry %s
                Keywords: %s
                Info: %s
                
                """.formatted(
                        entryInfo.getAssignment(ENTRY.NAME).orElse("No name"),
                        keywords,
                        entryInfo
        );
    }
}
