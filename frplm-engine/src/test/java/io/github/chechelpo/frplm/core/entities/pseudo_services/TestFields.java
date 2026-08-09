package io.github.chechelpo.frplm.core.entities.pseudo_services;

import io.github.chechelpo.frplm.core.entities.fields.EntityFieldsValidator;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.jooq.generated.tables.records.TestTableRecord;
import org.jspecify.annotations.NonNull;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.TEST_TABLE;

final class TestFields extends EntityFieldsValidator<TestTableRecord> {
    @Override
    protected List<FieldInfo<TestTableRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(TEST_TABLE.FIRST_ID)
                        .key()
                        .readOnly()
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(TEST_TABLE.SECOND_ID)
                        .key()
                        .readOnly()
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(TEST_TABLE.NAME)
                        .requireOnCreate()
                        .build()
        );
    }

    @Override
    public List<EntityKey<TestTableRecord>> keysOf(@NonNull List<TestTableRecord> records) {
        return super.keysOf(records);
    }
}
