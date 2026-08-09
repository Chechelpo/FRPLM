package io.github.chechelpo.frplm.domain.prolog.predicates;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

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