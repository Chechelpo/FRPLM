package chechelpo.frplm.domain.lorebook.core;

import chechelpo.frplm.domain.character.core.CharacterCoreTestContext;
import chechelpo.frplm.domain.lorebook.outlet.OutletTestContext;
import chechelpo.frplm.jooq.generated.tables.Lorebooks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Sql(
        scripts = "classpath:db/schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Import(LorebookTestContext.class)
class LorebookServiceTest {
    @Autowired
    LorebookTestContext testContext;
    @Autowired
    CharacterCoreTestContext characters;

    @BeforeEach
    void setUp() {
        testContext.reload();
    }

}