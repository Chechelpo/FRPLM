package chechelpo.frplm.domain.connection.api_keys;

import chechelpo.frplm.domain.lorebook.core.LorebookTestContext;
import org.junit.jupiter.api.Test;
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
@Import({SecretServiceTestContext.class})
class SecretServiceTest {
    @Autowired SecretServiceTestContext secretService;

    @Test
    void registerNewKey() {

    }

    @Test
    void getKeyForConnectionHost() {
    }

    @Test
    void hasApiKey() {
    }
}