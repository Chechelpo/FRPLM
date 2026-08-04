package io.github.chechelpo.frplm.domain.sessions.messages;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.domain.sessions.core.SessionTestContext;
import io.github.chechelpo.frplm.domain.world.location.LocationTestContext;
import io.github.chechelpo.frplm.extensions.api.session.ChatMessage;
import io.github.chechelpo.frplm.interfaces.DBReload;
import io.github.chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRole;
import io.github.chechelpo.frplm.test_utils.Asserts;
import org.jooq.Field;
import org.jooq.TableField;
import org.jooq.TableRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.MESSAGES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestComponent
@Import({SessionTestContext.class, LocationTestContext.class})
public class MessageTestContext implements DBReload {

    @Autowired
    public SessionTestContext sessions;
    @Autowired
    public MessageService service;
    @Autowired
    ResponseFields responseFields;
    @Autowired
    ResponseService responseService;

    @Autowired
    MessageFieldsHelper fields;
    @Autowired
    LocationTestContext locations;


    @Override
    public void reload() {
        sessions.reload();
        locations.reload();
    }

    public record Context(SessionTestContext.SessionContext sessionContext, List<MessagesRecord> messages) {
    }

    public Context createSessionWithMessages(int locationAmount, int messageAmount){
        return createSessionWithMessages(locationAmount, messageAmount, 3);
    }

    public Context createSessionWithMessages(int locationAmount, int messageAmount, int charactersPerLocation) {
        SessionTestContext.SessionContext sessionContext = sessions.createSession(locationAmount, charactersPerLocation);
        SessionsRecord session = sessionContext.session();

        List<EntityKey<MessagesRecord>> createdMessagesKeys = new ArrayList<>(messageAmount);
        for (int i = 0; i < messageAmount; i++) {
            EntityDataPayload<MessagesRecord> newMessage = EntityDataPayload.<MessagesRecord>builder()
                    .set(MESSAGES.SESSION_ID, session.getId())
                    .set(MESSAGES.CONTENT, "ContentOfMessage_" + i)
                    .build();
            if (i % 2 == 0) newMessage.set(MESSAGES.ROLE, ChatCompletionRole.ASSISTANT.wireValue());
            else newMessage.set(MESSAGES.ROLE, ChatCompletionRole.USER.wireValue());

            createdMessagesKeys.add(service.keyOf(service.createAndGet(newMessage)));
        }
        SessionTestContext.SessionContext updated = new SessionTestContext.SessionContext(
                sessionContext.userCharacter(),
                sessions.service.find(sessions.service.keyOf(sessionContext.session())).orElseThrow(),
                sessionContext.sessionLocations()
        );

        return new Context(updated,
                createdMessagesKeys.stream()
                        .map(key -> service.find(key).orElseThrow(notFound ->
                                new IllegalStateException("Couldn't find message " + notFound.toString()))
                        )
                        .toList()
                );
    }

    public void assertMessageEquals(MessagesRecord expected, MessagesRecord actual, boolean includeKeys){
        Set<TableField<MessagesRecord, ?>> keyFields = includeKeys ? fields.keyFields() : Set.of();
        Asserts.assertEqualsMinusFields(expected, actual, keyFields);
    }


}
