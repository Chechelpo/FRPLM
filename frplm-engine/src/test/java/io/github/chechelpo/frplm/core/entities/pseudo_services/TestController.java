package io.github.chechelpo.frplm.core.entities.pseudo_services;

import io.github.chechelpo.frplm.jooq.generated.tables.records.TestTableRecord;

class TestController extends EntityController<TestTableRecord, TestService> {
    TestController(TestService service, DTOMapper<TestTableRecord> mapper) {
        super(service, mapper);
    }
}
