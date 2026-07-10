package io.github.chechelpo.frplm.core.entities.pseudo_services;

import chechelpo.frplm.jooq.generated.tables.records.TestTableRecord;

final class TestControllerFields extends ABSControllerAwareHelper<TestTableRecord, TestService, TestController> {
    TestControllerFields(TestService service, TestController controller) {
        super(service, controller);
    }
}
