package io.github.chechelpo.frplm.core.entities.pseudo_services;

import io.github.chechelpo.frplm.jooq.generated.tables.records.TestTableRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import static io.github.chechelpo.frplm.jooq.generated.Tables.TEST_TABLE;

public class TestStore extends EntityStore<TestTableRecord> {
    protected TestStore(@NotNull DSLContext ctx) {
        super(ctx, TEST_TABLE, EntityConfigs.Types.TEST_ENTITY);
    }
}
