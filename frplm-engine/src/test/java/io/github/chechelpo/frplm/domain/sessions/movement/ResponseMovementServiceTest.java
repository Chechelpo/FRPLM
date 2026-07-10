package io.github.chechelpo.frplm.domain.sessions.movement;

import io.github.chechelpo.frplm.domain.sessions.messages.MessageTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@Sql(
        scripts = "classpath:db/schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Import({MessageTestContext.class})
class ResponseMovementServiceTest {
    @Autowired MessageTestContext messageTestContext;

    @BeforeEach
    void setUp() {
        messageTestContext.reload();
    }

    @Test
    void getResponseMovements() {
        int locationAmount = 100;
        MessageTestContext.Context context = messageTestContext.createSessionWithMessages(locationAmount, 3);
    }
}