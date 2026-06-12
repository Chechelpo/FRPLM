package chechelpo.frplm.domain.sessions.messages.gen;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.domain.character.starting_locations.StartingLocationTestContext;
import chechelpo.frplm.domain.sessions.core.SessionTestContext;
import chechelpo.frplm.domain.sessions.messages.core.MessageTestContext;
import chechelpo.frplm.jooq.generated.tables.records.*;
import chechelpo.frplm.openai_compatible.ChatCompletionRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static chechelpo.frplm.jooq.generated.Tables.LLM_GEN;
import static chechelpo.frplm.jooq.generated.Tables.MESSAGES;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Sql(
        scripts = "classpath:db/schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Import({SessionTestContext.class, ResponsesTestContext.class, MessageTestContext.class})
class GenServiceTest {
    @Autowired
    private StartingLocationTestContext startingLocations;
    @Autowired
    private MessageTestContext messages;
    @Autowired
    SessionTestContext sessions;
    @Autowired
    ResponsesTestContext responses;


    @BeforeEach
    void setUp() {
        sessions.reload();
        responses.reload();
        startingLocations.reload();
    }

    @Test
    void getLastResponse() {
        SessionTestContext.SessionContext sessionContext = sessions.createSession(10, 10);
        SessionsRecord session = sessionContext.session();
        CharactersRecord userCharacter = sessionContext.userCharacter();
        LocationsRecord userCharacterStartingLocation = startingLocations.service.startingLocationAt(userCharacter, session.getWorldId())
                .getFirst();
        String messageContent = "This is the first generated message content";

        MessagesRecord secondMessage = responses.messages.service.createAndGet(EntityDataPayload.<MessagesRecord>builder()
                .set(MESSAGES.SESSION_ID, session.getId())
                .set(MESSAGES.WORLD_ID, session.getWorldId())
                .set(MESSAGES.LOCATION_ID, userCharacterStartingLocation.getId())
                .set(MESSAGES.CONTENT, messageContent)
                .set(MESSAGES.ROLE, ChatCompletionRole.ASSISTANT.wireValue())
                .build()
        );

        Optional<ResponsesRecord> activeResponse = responses.genService.getActiveResponseOf(secondMessage);
        assertTrue(activeResponse.isPresent(), "No active response for generated message");

        assertEquals(messageContent, activeResponse.get().getContent(),
                "Wrong message content. Expected: %s, Actual: %s".formatted(messageContent, activeResponse.get().getContent())
        );

        MessagesRecord userMessage =  responses.messages.service.createAndGet(EntityDataPayload.<MessagesRecord>builder()
                .set(MESSAGES.SESSION_ID, session.getId())
                .set(MESSAGES.WORLD_ID, session.getWorldId())
                .set(MESSAGES.LOCATION_ID, userCharacterStartingLocation.getId())
                .set(MESSAGES.CONTENT, messageContent)
                .set(MESSAGES.ROLE, ChatCompletionRole.USER.wireValue())
                .build()
        );

        Optional<ResponsesRecord> supposedUserResponse = responses.genService.getActiveResponseOf(userMessage);

        assertFalse(supposedUserResponse.isPresent(), "User generated messages shouldn't have a response record");
    }

    @Test
    void generateNewResponse_normalLifeCycle(){
        SessionTestContext.SessionContext sessionContext = sessions.createSession(10, 10);
        SessionsRecord session = sessionContext.session();
        CharactersRecord userCharacter = sessionContext.userCharacter();
        LocationsRecord userCharacterStartingLocation = startingLocations.service.startingLocationAt(userCharacter, session.getWorldId())
                .getFirst();
        String messageContent = "This is the first generated message content";

        MessagesRecord firstMessage = responses.messages.service.createAndGet(EntityDataPayload.<MessagesRecord>builder()
                .set(MESSAGES.SESSION_ID, session.getId())
                .set(MESSAGES.WORLD_ID, session.getWorldId())
                .set(MESSAGES.LOCATION_ID, userCharacterStartingLocation.getId())
                .set(MESSAGES.CONTENT, messageContent)
                .set(MESSAGES.ROLE, ChatCompletionRole.ASSISTANT.wireValue())
                .build()
        );

        EntityKey<LlmGenRecord> generatedKey = EntityKey.<LlmGenRecord>builder()
                .set(LLM_GEN.SESSION_ID, session.getId())
                .set(LLM_GEN.TICK_NUM, firstMessage.getTickNum())
                .build();

        int newGeneratedAmount = 100;
        List<String> newContents = new ArrayList<>(newGeneratedAmount);
        for (int i = 0; i < newGeneratedAmount; i++) newContents.add("This is generation number " + i);

        for (String expectedContent : newContents) {
            responses.genService.registerNewResponse(generatedKey, expectedContent);
            Optional<ResponsesRecord> responsesRecord = responses.genService.getActiveResponseOf(firstMessage);
            assertTrue(responsesRecord.isPresent(), "No active response for generated message");

            assertEquals(expectedContent, responsesRecord.get().getContent(), "Wrong message content");
        }
    }

    @Test
    void generateNewResponse_RejectsResponsesForOlderMessages(){
        int messageAmount = 1000;
        MessageTestContext.Context context = messages.createSessionWithMessages(100, messageAmount);

        int sessionId = context.sessionContext().session().getId();
        for (int i = 0; i < messageAmount - 1; i++) {
            int finalI = i;
            assertThrows(
                    RuntimeException.class,
                    () -> responses.genService.registerNewResponse(
                    EntityKey.<LlmGenRecord>builder()
                            .set(LLM_GEN.SESSION_ID, sessionId)
                            .set(LLM_GEN.TICK_NUM, finalI)
                            .build(),
                    "Ignored"),
                    "Could add a response to an older message"
            );
        }
    }
}