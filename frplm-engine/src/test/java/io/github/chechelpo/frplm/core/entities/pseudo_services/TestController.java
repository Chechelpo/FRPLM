package io.github.chechelpo.frplm.core.entities.pseudo_services;

import io.github.chechelpo.frplm.core.entities.fields.DTOMapper;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.TestTableRecord;

class TestController extends EntityController<TestTableRecord, TestService> {
    TestController(TestService service, DTOMapper<TestTableRecord> mapper) {
        super(EntityConfigs.Types.TEST_ENTITY, service, mapper);
    }
}
