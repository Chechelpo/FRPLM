package io.github.chechelpo.frplm.test_utils.fixtures;

import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import org.jooq.TableField;
import org.jspecify.annotations.NonNull;

import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LorebookFixtures extends EntityFixtures<LorebooksRecord, LorebookService> {
    public LorebookFixtures(LorebookService service, @NonNull String seed) {
        super(service, seed);
    }

    @Override
    protected Set<TableField<LorebooksRecord, ?>> doNotGenerateFields() {
        return Set.of(LOREBOOKS.NEXT_ENTRY_ID);
    }

    public LorebooksRecord assertLorebookExists(int lorebookId){
        var result = service().find(EntityKey.of(LOREBOOKS.ID, lorebookId));
        assertTrue(result.isFound(), "Lorebook does not exist with id " + lorebookId);

        return result.get();
    }
}
