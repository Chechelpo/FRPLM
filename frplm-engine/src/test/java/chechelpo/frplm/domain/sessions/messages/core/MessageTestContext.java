package chechelpo.frplm.domain.sessions.messages.core;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.domain.sessions.core.SessionTestContext;
import chechelpo.frplm.domain.world.location.LocationTestContext;
import chechelpo.frplm.interfaces.DBReload;
import chechelpo.frplm.jooq.generated.tables.Messages;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import chechelpo.frplm.openai_compatible.ChatCompletionRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.MESSAGES;

@TestComponent
@Import({SessionTestContext.class, LocationTestContext.class})
public class MessageTestContext implements DBReload {
    @Autowired
    public SessionTestContext sessions;
    @Autowired
    public MessageService service;
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

    public Context createSessionWithMessages(int locationAmount, int messageAmount) {
        SessionTestContext.SessionContext sessionContext = sessions.createSession(locationAmount, 3);
        SessionsRecord session = sessionContext.session();

        List<MessagesRecord> createdMessages = new ArrayList<>(messageAmount);
        for (int i = 0; i < messageAmount; i++) {
            EntityDataPayload<MessagesRecord> newMessage = EntityDataPayload.<MessagesRecord>builder()
                    .set(MESSAGES.SESSION_ID, session.getId())
                    .set(MESSAGES.CONTENT, "ContentOfMessage_" + i)
                    .build();
            if (i % 2 == 0) newMessage.set(MESSAGES.ROLE, ChatCompletionRole.ASSISTANT.wireValue());
            else newMessage.set(MESSAGES.ROLE, ChatCompletionRole.USER.wireValue());

            createdMessages.add(service.createAndGet(newMessage));
        }

        return new Context(sessionContext, createdMessages);
    }
}
