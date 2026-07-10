package io.github.chechelpo.frplm.core.entities.pseudo_services;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import io.github.chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.jooq.generated.tables.records.TestTableRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import static chechelpo.frplm.jooq.generated.Tables.TEST_TABLE;

public class TestStore extends EntityStore<TestTableRecord> {
    protected TestStore(@NotNull DSLContext ctx) {
        super(ctx, TEST_TABLE, EntityTypes.Types.TEST_ENTITY);
    }
}
