package chechelpo.frplm.domain.sessions.movement;

import chechelpo.frplm.domain.sessions.messages.core.MessageTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import({MessageTestContext.class})
class CurrentLocationServiceTest {

    @Autowired MessageTestContext messages;

    @BeforeEach
    void setUp() {
        messages.reload();
    }
}