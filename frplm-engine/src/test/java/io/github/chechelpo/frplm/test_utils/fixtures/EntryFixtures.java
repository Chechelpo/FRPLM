package io.github.chechelpo.frplm.test_utils.fixtures;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.domain.lorebook.entry.core.EntryService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import org.jooq.TableField;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class EntryFixtures extends EntityFixtures<EntryRecord, EntryService> {
    EntryFixtures(EntryService service, EntityFixtureFactory fixtures, @NonNull String seed) {
        super(service, fixtures, seed);
    }

    @Override
    protected Set<TableField<EntryRecord, ?>> doNotGenerateFields() {
        return Set.of();
    }

    @Override
    protected List<Consumer<EntityDataPayload<EntryRecord>>> getFunctionsToAssignForeignFields(EntityDataPayload<EntryRecord> sample) {
        
        return List.of();
    }
}
