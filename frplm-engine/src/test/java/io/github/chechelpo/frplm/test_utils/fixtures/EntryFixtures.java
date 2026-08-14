package io.github.chechelpo.frplm.test_utils.fixtures;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.domain.lorebook.entry.core.EntryService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import org.jooq.TableField;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static io.github.chechelpo.frplm.jooq.generated.Tables.ENTRY;
import static io.github.chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;

public class EntryFixtures extends EntityFixtures<EntryRecord, EntryService> {
    private final LorebookFixtures lorebookFixtures;
    EntryFixtures(EntryService service, EntityFixtureFactory fixtures, @NonNull String seed) {
        super(service, fixtures, seed);
        this.lorebookFixtures = fixtures.lorebook(seed);
    }

    @Override
    protected Set<TableField<EntryRecord, ?>> doNotGenerateFields() {
        return Set.of();
    }

    @Override
    protected DoActions<EntryRecord> getFunctionsToAssignForeignFields(@NonNull EntityDataPayload<EntryRecord> sample) {
        DoActions<EntryRecord> doActions = DoActions.instantiate(1);

        sample.getAssignment(ENTRY.LOREBOOK_ID)
                .ifUnassignedRun(
                        () ->  {
                            LorebooksRecord lorebook = lorebookFixtures.addAndCreateTo(EntityDataPayload.empty());
                            doActions.add(
                                    payload ->
                                            payload.set(ENTRY.LOREBOOK_ID, LOREBOOKS.ID, lorebook)
                            );
                        }
                );

        return doActions;
    }
}
