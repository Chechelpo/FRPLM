package io.github.chechelpo.frplm.extensions.implementations.session;

import io.github.chechelpo.frplm.domain.sessions.messages.MessageService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SessionImplTest {

    @Test
    void getLastMessagesRange() {
        MessageService messageService = mock(MessageService.class);
    }
}