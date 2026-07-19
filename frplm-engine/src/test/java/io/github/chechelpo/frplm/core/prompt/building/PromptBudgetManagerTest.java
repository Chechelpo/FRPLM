package io.github.chechelpo.frplm.core.prompt.building;

import io.github.chechelpo.frplm.core.prompt.TextType;
import io.github.chechelpo.frplm.extensions.api.session.ChatMessage;
import io.github.chechelpo.frplm.extensions.api.session.Session;
import io.github.chechelpo.frplm.extensions.api.standalone.PromptSnapshot;
import io.github.chechelpo.frplm.extensions.api.utils.PromptBudget;
import io.github.chechelpo.frplm.utils.tokenizers.TokenizationMode;
import io.github.chechelpo.frplm.utils.tokenizers.TokenizerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromptBudgetManagerTest {

    private static final String MODEL_ID = "test-model";

    @Mock
    private TokenizerService tokenizer;

    @Mock
    private Session session;

    private PromptBudgetManager manager;

    @BeforeEach
    void setUp() {
        manager = new PromptBudgetManager(
                MODEL_ID,
                new PromptBudget(100, 0.20F, 0.30F),
                tokenizer
        );
    }

    private ChatMessage message(String content) {
        ChatMessage message = mock(ChatMessage.class);
        when(message.content()).thenReturn(content);
        return message;
    }

    @Test
    void fillChatHistoryBudgetStopsBeforeExceedingBudget() {
        ChatMessage tick12 = message("tick-12");
        ChatMessage tick11 = message("tick-11");
        ChatMessage tick10 = message("tick-10");

        when(session.getCurrentTick()).thenReturn(12);

        when(session.getLastMessagesRange(3, 12))
                .thenReturn(List.of(tick12, tick11, tick10));

        when(tokenizer.tokenCount(
                MODEL_ID,
                "tick-12",
                TokenizationMode.RAW_TEXT
        )).thenReturn(12);

        when(tokenizer.tokenCount(
                MODEL_ID,
                "tick-11",
                TokenizationMode.RAW_TEXT
        )).thenReturn(10);

        when(tokenizer.tokenCount(
                MODEL_ID,
                "tick-10",
                TokenizationMode.RAW_TEXT
        )).thenReturn(9);

        List<ChatMessage> actual =
                manager.fillChatHistoryBudget(session);

        assertEquals(
                List.of(tick11, tick12),
                actual
        );

        InOrder order = inOrder(session);
        order.verify(session).getCurrentTick();
        order.verify(session).getLastMessagesRange(3, 12);

        verifyNoMoreInteractions(session);
    }

    @Test
    void fillChatHistoryBudgetReadsRangesBackwardAndReturnsChronologicalOrder() {
        List<ChatMessage> chronological = IntStream.rangeClosed(0, 14)
                .mapToObj(tick -> message("tick-" + tick))
                .toList();

        List<ChatMessage> firstBatch =
                new ArrayList<>(chronological.subList(5, 15));
        Collections.reverse(firstBatch);

        List<ChatMessage> secondBatch =
                new ArrayList<>(chronological.subList(0, 5));
        Collections.reverse(secondBatch);

        when(session.getCurrentTick()).thenReturn(14);

        when(session.getLastMessagesRange(5, 14))
                .thenReturn(firstBatch);

        when(session.getLastMessagesRange(0, 4))
                .thenReturn(secondBatch);

        when(tokenizer.tokenCount(
                eq(MODEL_ID),
                anyString(),
                eq(TokenizationMode.RAW_TEXT)
        )).thenReturn(1);

        List<ChatMessage> actual =
                manager.fillChatHistoryBudget(session);

        assertEquals(chronological, actual);

        InOrder order = inOrder(session);
        order.verify(session).getCurrentTick();
        order.verify(session).getLastMessagesRange(5, 14);
        order.verify(session).getLastMessagesRange(0, 4);

        verifyNoMoreInteractions(session);

        verify(tokenizer, times(15)).tokenCount(
                eq(MODEL_ID),
                anyString(),
                eq(TokenizationMode.RAW_TEXT)
        );
    }
    @Test
    void fillChatHistoryBudgetSkipsNullAndBlankContent() {
        ChatMessage newest = message("newest");
        ChatMessage blank = message("   ");
        ChatMessage nullContent = message(null);
        ChatMessage oldest = message("oldest");

        when(session.getCurrentTick()).thenReturn(3);

        when(session.getLastMessagesRange(0, 3))
                .thenReturn(List.of(
                        newest,
                        blank,
                        nullContent,
                        oldest
                ));

        when(tokenizer.tokenCount(
                MODEL_ID,
                "newest",
                TokenizationMode.RAW_TEXT
        )).thenReturn(5);

        when(tokenizer.tokenCount(
                MODEL_ID,
                "oldest",
                TokenizationMode.RAW_TEXT
        )).thenReturn(5);

        List<ChatMessage> actual =
                manager.fillChatHistoryBudget(session);

        assertEquals(
                List.of(oldest, newest),
                actual
        );

        verify(tokenizer).tokenCount(
                MODEL_ID,
                "newest",
                TokenizationMode.RAW_TEXT
        );

        verify(tokenizer).tokenCount(
                MODEL_ID,
                "oldest",
                TokenizationMode.RAW_TEXT
        );

        verifyNoMoreInteractions(tokenizer);
    }
    @Test
    void unusedChatHistoryBudgetIsTransferredToGeneralBudget() {
        ChatMessage message = message("history");

        when(session.getCurrentTick()).thenReturn(0);
        when(session.getLastMessagesRange(0, 0))
                .thenReturn(List.of(message));

        when(tokenizer.tokenCount(
                MODEL_ID,
                "history",
                TokenizationMode.RAW_TEXT
        )).thenReturn(20);

        manager.fillChatHistoryBudget(session);

        when(tokenizer.tokenCount(
                MODEL_ID,
                "general-section",
                TokenizationMode.RAW_TEXT
        )).thenReturn(60);

        assertTrue(manager.hasSpaceFor(
                "general-section",
                TextType.PROMPT_SECTION
        ));
    }
}