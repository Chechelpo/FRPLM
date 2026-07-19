package io.github.chechelpo.frplm.domain.prolog.predicates;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.jooq.generated.tables.records.PrologPredicateRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import static io.github.chechelpo.frplm.jooq.generated.Tables.PROLOG_PREDICATE;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Sql(
        scripts = "classpath:db/schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Import(PrologPredicateTestContext.class)
class PrologPredicateServiceTest {
    @Autowired
    PrologPredicateTestContext prologPredicateTestContext;

}