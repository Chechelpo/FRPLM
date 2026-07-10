package io.github.chechelpo.frplm.core.entities.pseudo_services;

import chechelpo.frplm.jooq.generated.tables.records.TestTableRecord;

final class TestFields extends ABSHelper<TestTableRecord, TestService> {
    TestFields(TestService service) {
        super(service);
    }
}
