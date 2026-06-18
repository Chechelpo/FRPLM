package chechelpo.frplm.domain.sessions.movement;

import chechelpo.frplm.domain.character.core.CharacterCoreTestContext;
import chechelpo.frplm.domain.character.starting_locations.StartingLocationTestContext;
import chechelpo.frplm.domain.sessions.core.SessionTestContext;
import chechelpo.frplm.domain.sessions.messages.MessageTestContext;
import chechelpo.frplm.domain.world.location.LocationTestContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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