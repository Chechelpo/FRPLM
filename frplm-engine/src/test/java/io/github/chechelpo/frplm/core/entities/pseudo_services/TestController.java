package io.github.chechelpo.frplm.core.entities.pseudo_services;

import chechelpo.frplm.jooq.generated.tables.records.TestTableRecord;

class TestController extends EntityController<TestTableRecord, TestService> {
    TestController(TestService service) {
        super(service, false);
    }
}
