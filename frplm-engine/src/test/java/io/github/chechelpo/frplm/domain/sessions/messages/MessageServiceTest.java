package io.github.chechelpo.frplm.domain.sessions.messages;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.domain.character.core.CharacterCoreTestContext;
import io.github.chechelpo.frplm.domain.character.starting_locations.StartingLocationTestContext;
import io.github.chechelpo.frplm.domain.sessions.core.SessionTestContext;
import io.github.chechelpo.frplm.domain.world.location.LocationTestContext;
import io.github.chechelpo.frplm.exceptions.runtime.InvalidValue;
import io.github.chechelpo.frplm.jooq.generated.tables.records.*;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Sql(
        scripts = "classpath:db/schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Import({CharacterCoreTestContext.class, StartingLocationTestContext.class, LocationTestContext.class, SessionTestContext.class, MessageTestContext.class})
class MessageServiceTest {
    @Autowired
    CharacterCoreTestContext characters;
    @Autowired
    StartingLocationTestContext startingLocations;
    @Autowired
    LocationTestContext locations;
    @Autowired
    MessageService messageService;
    @Autowired
    MessageFieldsHelper fields;
    @Autowired
    LocationTestContext locationContext;
    @Autowired
    SessionTestContext sessionContext;
    @Autowired
    private MessageTestContext messageTestContext;

    @BeforeEach
    void setUp() {
        characters.reload();
        locations.reload();
    }

    @Test
    void testFirstMessageCreation() {
        CharactersRecord character = characters.createAndGetRecords(1).getFirst();
        LocationsRecord location = locations.createAndGetTestLocationsOfSameWorld(1).getFirst();
        String firstMessage = "First message";

        characters.service.update(
                characters.service.keyOf(character),
                EntityDataPayload.<CharactersRecord>builder()
                        .set(CHARACTERS.CAN_BE_USER, true)
                        .set(CHARACTERS.WELCOME_MESSAGE, firstMessage)
                        .build()
        );

        startingLocations.service.createAndGet(EntityDataPayload.<StartingLocationsRecord>builder()
                .set(STARTING_LOCATIONS.WORLD_ID, location.getWorldId())
                .set(STARTING_LOCATIONS.LOCATION_ID, location.getId())
                .set(STARTING_LOCATIONS.CHARACTER_ID, character.getId())
                .build()
        );

        int sessionId = sessionContext.service.createAndGet(
                EntityDataPayload.<SessionsRecord>builder()
                        .set(SESSIONS.NAME, "TestSession")
                        .set(SESSIONS.USER_PERSONA_ID, character.getId())
                        .set(SESSIONS.WORLD_ID, location.getWorldId())
                        .build(),
                SESSIONS.ID
        );
        MessagesRecord message = messageService.getLastMessageOf(sessionId);

        assertEquals(firstMessage, message.getContent());
    }

    @Test
    void delete_rejectsDeletionOfOlderMessages() {
        int messageAmount = 1000;
        MessageTestContext.Context context = messageTestContext.createSessionWithMessages(100, messageAmount);

        int sessionId = context.sessionContext().session().getId();
        for (int i = 0; i < messageAmount - 1; i++) {
            int finalI = i;
            assertThrows(
                    InvalidValue.class,
                    () -> messageTestContext.service.delete(EntityKey.<MessagesRecord>builder()
                            .set(MESSAGES.SESSION_ID, sessionId)
                            .set(MESSAGES.TICK_NUM, finalI)
                            .build()
                    ),
                    "Could delete message " + i
            );
        }
    }

    @Test
    void delete_acceptsNormalDeletions() {
        int messageAmount = 1000;
        MessageTestContext.Context context = messageTestContext.createSessionWithMessages(100, messageAmount);

        int sessionId = context.sessionContext().session().getId();
        /*
        Important: This must start from message amount + 1 cause the engine hands out weak ids based on increment and get so
        the first id is actually 1 and not 0.
         */
        for (int i = messageAmount + 1; i > 0; i--)
            assertTrue(
                    messageTestContext.service.delete(
                            EntityKey.<MessagesRecord>builder()
                                    .set(MESSAGES.SESSION_ID, sessionId)
                                    .set(MESSAGES.TICK_NUM, i)
                                    .build()
                    ),
                    "Couldn't delete message " + i
            );
    }

    @Test
    void create_deniesPromptsForUserMessages() {
        MessageTestContext.Context context = messageTestContext.createSessionWithMessages(100, 100);

        int sessionId = context.sessionContext().session().getId();
        assertThrows(
                InvalidValue.class,
                () -> messageService.createAndGet(EntityDataPayload.<MessagesRecord>builder()
                        .set(MESSAGES.SESSION_ID, sessionId)
                        .set(MESSAGES.REQUEST_JSON, "This should throw")
                        .set(MESSAGES.CONTENT, "Test")
                        .set(MESSAGES.ROLE, ChatCompletionRole.USER.wireValue())
                        .build()
                ),
                "Could create a user message with a prompt value"
        );
    }

    @Test
    void create_deniesReasoningForUserMessages() {
        MessageTestContext.Context context = messageTestContext.createSessionWithMessages(1, 1);
        int sessionId = context.sessionContext().session().getId();
        assertThrows(
                InvalidValue.class,
                () -> messageService.createAndGet(EntityDataPayload.<MessagesRecord>builder()
                        .set(MESSAGES.SESSION_ID, sessionId)
                        .set(MESSAGES.REASONING, "Aa")
                        .set(MESSAGES.CONTENT, "test")
                        .set(MESSAGES.ROLE, ChatCompletionRole.USER.wireValue())
                        .build()
                )
        );
    }

    @Test
    void onCreate_ResponseCreated() {
        MessageTestContext.Context context = messageTestContext.createSessionWithMessages(100, 100);

        List<MessagesRecord> messages = context.messages();

        for (MessagesRecord message : messages) {
            System.out.printf("Checking message tick %s. Response number %s %n", message.getTickNum(), message.getResponseNum());
            assertTrue(message.getResponseNum() > 0, "This message has no responses");

            ResponsesRecord activeResponse = messageTestContext.service.getActiveResponseOf(message);

            assertEquals(activeResponse.getContent(), message.getContent());
            assertEquals(activeResponse.getResponseNum(), message.getActiveResponse());
        }
    }

    @Test
    void newResponse_rejectsUserMessages() {
        MessageTestContext.Context context = messageTestContext.createSessionWithMessages(1, 1);
        MessagesRecord message = context.messages().getFirst();
        EntityKey<MessagesRecord> key = messageService.keyOf(message);

        messageService.update(key, EntityDataPayload.of(MESSAGES.ROLE, ChatCompletionRole.USER.wireValue()));
        assertThrows(
                InvalidValue.class,
                () -> messageService.registerNewResponse(message.getSessionId(), message.getTickNum(), "this should throw"),
                "Could register a new response for a user message"
        );
    }

    @Test
    void messageUpdate_UpdatesActiveResponse() {
        MessageTestContext.Context context = messageTestContext.createSessionWithMessages(100, 100);

        List<MessagesRecord> messages = context.messages();

        for (MessagesRecord message : messages) {
            System.out.printf("Checking message tick %s. Response number %s %n", message.getTickNum(), message.getResponseNum());

            EntityKey<MessagesRecord> key = messageTestContext.service.keyOf(message);
            ResponsesRecord previousActiveResponse = messageTestContext.service.getActiveResponseOf(key);

            String newContent = "New content: " + ThreadLocalRandom.current().nextInt();
            assertTrue(
                    messageTestContext.service.update(key, EntityDataPayload.of(MESSAGES.ROLE, ChatCompletionRole.ASSISTANT.wireValue())).success(),
                    "Could not change message role to assistant"
            );
            messageTestContext.service.registerNewResponse(message.getSessionId(), message.getTickNum(), newContent);
            messageTestContext.service.update(key, EntityDataPayload.of(MESSAGES.CONTENT, newContent));

            //Important, must be key or else it'll have a stale response number
            ResponsesRecord newActiveResponse = messageTestContext.service.getActiveResponseOf(key);

            assertNotEquals(previousActiveResponse.getResponseNum(), newActiveResponse.getResponseNum(), "Response num not changed");
            assertEquals(newContent, newActiveResponse.getContent(), "Content not updated"); //Updates active response
            assertNotEquals(newContent, previousActiveResponse.getContent(), "Updated content of previous response");
        }
    }

    @Test
    void changingUserMessageActiveResponse_Throws() {
        MessageTestContext.Context context = messageTestContext.createSessionWithMessages(100, 100);
        List<MessagesRecord> messages = context.messages();

        boolean atLeastOneUserMessage = false;
        for (MessagesRecord message : messages) {
            if (!message.getRole().equals(ChatCompletionRole.USER.wireValue())) continue;
            atLeastOneUserMessage = true;

            ResponsesRecord previousActiveResponse = messageTestContext.service.getActiveResponseOf(message);

            String newContent = "ignored";
            assertThrows(
                    InvalidValue.class,
                    () -> messageTestContext.service.registerNewResponse(message.getSessionId(), message.getTickNum(), newContent),
                    "Could register a new response for a user message"
            );
            ResponsesRecord activeResponse = messageTestContext.service.getActiveResponseOf(message);

            assertEquals(previousActiveResponse.getResponseNum(), activeResponse.getResponseNum(), "Changed active response");
        }

        assertTrue(atLeastOneUserMessage, "This test did nothing");
    }

    @Test
    void getLastMessageOfSession() {
        MessageTestContext.Context context =
                messageTestContext.createSessionWithMessages(2, 30);

        int sessionId = context.sessionContext().session().getId();
        int currentTick = context.sessionContext().session().getCurrentTick();

        MessagesRecord expected = context.messages().stream()
                .filter(message -> message.getTickNum() == currentTick)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No message exists for the session's current tick: " + currentTick
                ));

        MessagesRecord actual =
                messageTestContext.service.getLastMessageOf(sessionId);

        assertNotNull(actual, "Expected a message at the session's current tick");

        messageTestContext.assertMessageEquals(expected, actual, true);
    }

    @Test
    void getLastMessagesOfSession() {
        int number = 10;

        MessageTestContext.Context context =
                messageTestContext.createSessionWithMessages(2, 30);

        int sessionId = context.sessionContext().session().getId();

        List<MessagesRecord> expected = context.messages().stream()
                .sorted(Comparator.comparingInt(MessagesRecord::getTickNum).reversed())
                .limit(number)
                .sorted(Comparator.comparingInt(MessagesRecord::getTickNum))
                .toList();

        List<MessagesRecord> actual =
                messageTestContext.service.getLastMessagesOf(sessionId, number);

        assertEquals(expected.size(), actual.size(), "Mismatch in messages size");

        for (int i = 0; i < expected.size(); i++) {
            messageTestContext.assertMessageEquals(
                    expected.get(i),
                    actual.get(i),
                    true
            );
        }
    }

    @Test
    void getLastEnabled() {
        MessageTestContext.Context context =
                messageTestContext.createSessionWithMessages(2, 30);

        int sessionId = context.sessionContext().session().getId();

        // Disable the latest message so the filter is actually exercised
        MessagesRecord lastMessage = context.messages().stream()
                .max(Comparator.comparingInt(MessagesRecord::getTickNum))
                .orElseThrow();
        messageTestContext.service.update(
                messageTestContext.service.keyOf(lastMessage),
                EntityDataPayload.of(MESSAGES.IS_ENABLED, false)
        );

        // Re-fetch to get the updated IS_ENABLED state
        List<MessagesRecord> updated =
                messageTestContext.service.getMessages(context.sessionContext().session());

        MessagesRecord expected = updated.stream()
                .filter(message -> Boolean.TRUE.equals(message.getIsEnabled()))
                .max(Comparator.comparingInt(MessagesRecord::getTickNum))
                .orElseThrow(() -> new AssertionError("No enabled message found"));

        MessagesRecord actual =
                messageTestContext.service.getLastEnabled(sessionId);

        assertNotNull(actual, "Expected an enabled message");
        messageTestContext.assertMessageEquals(expected, actual, true);
    }

    @Test
    void getLastEnabledMessages() {
        int number = 20;
        MessageTestContext.Context context =
                messageTestContext.createSessionWithMessages(2, 40);

        int sessionId = context.sessionContext().session().getId();

        // Disable every 3rd message so the enabled filter is meaningful
        for (MessagesRecord message : context.messages()) {
            if (message.getTickNum() % 3 == 0) {
                messageTestContext.service.update(
                        messageTestContext.service.keyOf(message),
                        EntityDataPayload.of(MESSAGES.IS_ENABLED, false)
                );
            }
        }

        // Re-fetch to get the updated IS_ENABLED state
        List<MessagesRecord> updated =
                messageTestContext.service.getMessages(context.sessionContext().session());

        List<MessagesRecord> expected = updated.stream()
                .filter(message -> Boolean.TRUE.equals(message.getIsEnabled()))
                .sorted(Comparator.comparingInt(MessagesRecord::getTickNum).reversed())
                .limit(number)
                .sorted(Comparator.comparingInt(MessagesRecord::getTickNum))
                .toList();

        List<MessagesRecord> actual = messageTestContext.service.getLastEnabledMessages(sessionId, number);

        assertEquals(expected.size(), actual.size(), "Mismatch in messages size");

        for (int i = 0; i < expected.size(); i++) {
            messageTestContext.assertMessageEquals(
                    expected.get(i),
                    actual.get(i),
                    true
            );
        }
    }


    @Test
    void getMessageRange() {
        int fromTick = 3;
        int toTick = 10;

        MessageTestContext.Context context =
                messageTestContext.createSessionWithMessages(2, 30);

        List<MessagesRecord> expected = context.messages().stream()
                .filter(message -> message.getTickNum() >= fromTick)
                .filter(message -> message.getTickNum() <= toTick)
                .sorted(Comparator.comparingInt(MessagesRecord::getTickNum).reversed())
                .toList();

        int sessionId = context.sessionContext().session().getId();

        List<MessagesRecord> actual =
                messageTestContext.service.getRange(sessionId, fromTick, toTick);

        assertEquals(expected.size(), actual.size(), "Mismatch in messages size");

        for (int i = 0; i < expected.size(); i++) {
            messageTestContext.assertMessageEquals(
                    expected.get(i),
                    actual.get(i),
                    true
            );
        }
    }

    @Test
    void changingActiveResponse() {
        MessageTestContext.Context context = messageTestContext.createSessionWithMessages(2, 1);
        MessagesRecord message = context.messages().getFirst();
        EntityKey<MessagesRecord> key = messageTestContext.service.keyOf(message);

        ResponsesRecord response1 = messageTestContext.service.getActiveResponseOf(message);

        String activeResponse2Content = "response2";
        messageTestContext.service.registerNewResponse(message.getSessionId(), message.getTickNum(), activeResponse2Content);
        ResponsesRecord response2 = messageTestContext.service.getActiveResponseOf(message);

        String activeResponse3Content = "response3";
        messageTestContext.service.registerNewResponse(message.getSessionId(), message.getTickNum(), activeResponse3Content);
        ResponsesRecord response3 = messageTestContext.service.getActiveResponseOf(message);

        assertTrue(messageService.update(key, EntityDataPayload.of(MESSAGES.ACTIVE_RESPONSE, response1.getResponseNum())).success(),
                "Couldn't update active response"
        );
        ResponsesRecord firstActualResponse = messageTestContext.service.getActiveResponseOf(message);
        assertEquals(response1.getResponseNum(), firstActualResponse.getResponseNum(), "Response number not changed");
        assertEquals(response1.getContent(), firstActualResponse.getContent(), "Response content not changed");

        messageService.update(key, EntityDataPayload.of(MESSAGES.ACTIVE_RESPONSE, response2.getResponseNum()));
        ResponsesRecord secondActualResponse = messageTestContext.service.getActiveResponseOf(message);
        assertEquals(secondActualResponse.getResponseNum(), response3.getResponseNum(), "Response number not changed");
        assertEquals(secondActualResponse.getContent(), response2.getContent(), "Response content not changed");

        messageService.update(key, EntityDataPayload.of(MESSAGES.ACTIVE_RESPONSE, response3.getResponseNum()));
        ResponsesRecord thirdActualResponse = messageTestContext.service.getActiveResponseOf(message);
        assertEquals(thirdActualResponse.getResponseNum(), response3.getResponseNum(), "Response number not changed");
        assertEquals(thirdActualResponse.getContent(), response3.getContent(), "Response content not changed");
    }
}