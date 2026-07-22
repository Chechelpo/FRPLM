package io.github.chechelpo.frplm.extensions.implementations.session;

// ... existing code ...
import io.github.chechelpo.frplm.domain.character.core.CharacterService;
import io.github.chechelpo.frplm.domain.sessions.core.SessionService;
import io.github.chechelpo.frplm.domain.sessions.messages.MessageService;
import io.github.chechelpo.frplm.domain.sessions.movement.Movements;
import io.github.chechelpo.frplm.domain.world.core.WorldService;
import io.github.chechelpo.frplm.extensions.implementations.standalone.ExtensionContext;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SessionImplTest {
    MessageService mockService;
    SessionsRecord sessionRecord;
    SessionContext sessionContext;
    ExtensionContext extensionContext;
    SessionImpl session;

    @BeforeEach
    void setUp() {
        mockService = mock(MessageService.class);
        sessionRecord = new SessionsRecord();
        sessionRecord.setId(0);
        sessionRecord.setName("TestSession");

        WorldsRecord worldRecord = new WorldsRecord();
        worldRecord.setId(1);
        worldRecord.setName("TestWorld");

        CharactersRecord characterRecord = new CharactersRecord();
        characterRecord.setId(2);
        characterRecord.setName("TestCharacter");

        WorldService mockWorlds = mock(WorldService.class);
        when(mockWorlds.getWorldOf(sessionRecord)).thenReturn(worldRecord);

        CharacterService mockCharacters = mock(CharacterService.class);
        when(mockCharacters.getUserCharacter(sessionRecord)).thenReturn(characterRecord);

        extensionContext = mock(ExtensionContext.class);
        when(extensionContext.worlds()).thenReturn(mockWorlds);
        when(extensionContext.characters()).thenReturn(mockCharacters);

        sessionContext = new SessionContext(
                mock(Movements.class),
                mockService,
                mock(SessionService.class)
        );

        session = new SessionImpl(sessionRecord, extensionContext, sessionContext);
    }

    private static MessagesRecord message(int tickNum, boolean enabled) {
        MessagesRecord record = new MessagesRecord();
        record.setTickNum(tickNum);
        record.setContent("Message_" + tickNum);
        record.setIsEnabled(enabled);
        record.setRole("user");
        return record;
    }

    private static List<MessagesRecord> buildMessages(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(tick -> message(tick, tick % 3 != 0))
                .toList();
    }

    @Test
    void getCurrentTick_delegatesToRecord() {
        sessionRecord.setCurrentTick(7);
        assertEquals(7, session.getCurrentTick());
    }

    @Test
    void getChatHistory_noFilter_returnsAllMessages() {
        List<MessagesRecord> all = buildMessages(10);
        when(mockService.getMessages(sessionRecord)).thenReturn(all);

        var history = session.getChatHistory(false);

        assertEquals(10, history.size(), "All messages should be returned when filter is off");
    }

    @Test
    void getChatHistory_filtered_returnsOnlyEnabledMessages() {
        // buildMessages disables every 3rd tick (3, 6, 9) -> 7 enabled out of 10
        List<MessagesRecord> all = buildMessages(10);
        when(mockService.getMessages(sessionRecord)).thenReturn(all);

        var history = session.getChatHistory(true);

        assertEquals(7, history.size(), "Disabled messages should be excluded");
        assertTrue(history.stream().allMatch(msg ->
                        ((MessagesRecord) ((io.github.chechelpo.frplm.extensions.implementations.session.ChatMessageImpl) msg).getRecord())
                                .getIsEnabled()
                ),
                "All returned messages should be enabled"
        );
    }

    @Test
    void getChatHistory_empty_returnsEmptyList() {
        when(mockService.getMessages(sessionRecord)).thenReturn(List.of());

        var history = session.getChatHistory(true);

        assertNotNull(history);
        assertTrue(history.isEmpty());
    }

    @Test
    void getLastMessages_delegatesToGetLastMessagesOf_whenFilterDisabled() {
        List<MessagesRecord> lastMessages = buildMessages(5);
        when(mockService.getLastMessagesOf(0, 5)).thenReturn(lastMessages);

        var result = session.getLastMessages(5, false);

        assertEquals(5, result.size(), "Should return all fetched messages");
    }

    @Test
    void getLastMessages_delegatesToGetLastEnabledMessages_whenFilterEnabled() {
        List<MessagesRecord> lastEnabled = buildMessages(4);
        when(mockService.getLastEnabledMessages(0, 4)).thenReturn(lastEnabled);

        var result = session.getLastMessages(4, true);

        assertEquals(4, result.size(), "Should return all enabled messages");
    }

    @Test
    void getLastMessagesRange_delegatesToGetRange() {
        // tick 3..7 inclusive => 5 messages, descending (highest tick first)
        List<MessagesRecord> range = IntStream.rangeClosed(3, 7)
                .mapToObj(tick -> message(tick, true))
                .sorted(java.util.Comparator.comparingInt(MessagesRecord::getTickNum).reversed())
                .toList();
        when(mockService.getRange(0, 3, 7)).thenReturn(range);

        var result = session.getLastMessagesRange(3, 7);

        assertEquals(5, result.size(), "Range should return all messages in [3, 7]");
        // Order preserved from getRange (descending)
        assertEquals(7, ((MessagesRecord) ((io.github.chechelpo.frplm.extensions.implementations.session.ChatMessageImpl) result.get(0)).getRecord()).getTickNum());
        assertEquals(3, ((MessagesRecord) ((io.github.chechelpo.frplm.extensions.implementations.session.ChatMessageImpl) result.get(4)).getRecord()).getTickNum());
    }

    @Test
    void getLastMessagesRange_empty_returnsEmptyList() {
        when(mockService.getRange(0, 100, 200)).thenReturn(List.of());

        var result = session.getLastMessagesRange(100, 200);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getLastMessage_delegatesToGetLastEnabled_whenFilterEnabled() {
        MessagesRecord last = message(10, true);
        when(mockService.getLastEnabled(0)).thenReturn(last);

        var result = session.getLastMessage(true);

        assertNotNull(result);
        assertEquals(10, ((MessagesRecord) ((io.github.chechelpo.frplm.extensions.implementations.session.ChatMessageImpl) result).getRecord()).getTickNum());
    }

    @Test
    void getLastMessage_delegatesToGetLastMessageOf_whenFilterDisabled() {
        MessagesRecord last = message(10, false);
        when(mockService.getLastMessageOf(sessionRecord)).thenReturn(last);

        var result = session.getLastMessage(false);

        assertNotNull(result);
        assertEquals(10, ((ChatMessageImpl) result).getRecord().getTickNum());
    }
}