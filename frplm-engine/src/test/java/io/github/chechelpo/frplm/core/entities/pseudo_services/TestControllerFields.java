package io.github.chechelpo.frplm.core.entities.pseudo_services;

import io.github.chechelpo.frplm.core.entities.fields.EntityControllerFieldValidator;
import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.TestTableRecord;

import java.util.List;

final class TestControllerFields extends EntityControllerFieldValidator<TestTableRecord> {
    TestControllerFields() {
        super(EntityConfigs.Types.TEST_ENTITY);
    }

    @Override
    protected List<DTOField<TestTableRecord, ?>> getDTOStructure() {
        return List.of();
    }

    @Override
    protected List<FieldInfo<TestTableRecord, ?>> getCustom() {
        return List.of();
    }
}
