package io.github.chechelpo.frplm.core.entities.pseudo_services;

import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.fields.FieldValidator;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.jooq.generated.tables.records.TestTableRecord;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

class TestService extends EntityService<TestTableRecord, TestStore> {
    private List<EntityKey<TestTableRecord>> created_entities = new ArrayList<>();

    TestService(@NotNull TestStore store, FieldValidator<TestTableRecord> validator, @NotNull EventBus eventBus) {
        super(store, validator, eventBus);
    }

    @Override
    protected void afterSuccessfulCreate(TestTableRecord data, long operationID) {
        created_entities.add(keyOf(data));
        super.afterSuccessfulCreate(data, operationID);
    }

    void endTest(){
        created_entities.forEach(this::delete);
    }
}
