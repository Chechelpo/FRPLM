package chechelpo.frplm.utils.importers.lorebooks;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

import static chechelpo.frplm.jooq.generated.Tables.ENTRY;

public record NewEntryOrder(Set<String> keywords, EntityDataPayload<EntryRecord> entryInfo){
    @Override
    public @NotNull String toString() {
        return """
                
                Entry %s
                Keywords: %s
                Info: %s
                
                """.formatted(
                        entryInfo.getValue(ENTRY.NAME).orElse("No name"),
                        keywords,
                        entryInfo
        );
    }
}
