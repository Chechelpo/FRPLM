package io.github.chechelpo.frplm.core.prompt.building;

import io.github.chechelpo.frplm.extensions.api.prompts.PromptSection;
import io.github.chechelpo.frplm.extensions.api.session.ChatMessage;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionMessage;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRole;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.OUTLET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PromptRendererTest {

    private static final DSLContext JOOQ =
            DSL.using(SQLDialect.DEFAULT);

    @Test
    void sectionsAtSameDepthAreInsertedInReverseRegistrationOrder() {
        LorebooksManager lorebooksManager =
                mock(LorebooksManager.class);

        when(lorebooksManager.getOutlets())
                .thenReturn(emptyOutlets());

        SectionManager chatHistorySection =
                mock(SectionManager.class);

        when(chatHistorySection.getInjectionOrder())
                .thenReturn(relative(0));

        when(chatHistorySection.isChatHistorySection())
                .thenReturn(true);

        SectionManager firstRegistered =
                new SectionManager(
                        "First depth section",
                        ChatCompletionRole.SYSTEM,
                        atDepth(1)
                );

        SectionManager secondRegistered =
                new SectionManager(
                        "Second depth section",
                        ChatCompletionRole.SYSTEM,
                        atDepth(1)
                );

        List<ChatMessage> chatHistory = List.of(
                chatMessage("History 1"),
                chatMessage("History 2")
        );

        PromptRenderer renderer = new PromptRenderer(
                chatHistory,
                List.of(
                        chatHistorySection,
                        firstRegistered,
                        secondRegistered
                )
        );

        List<ChatCompletionMessage> result =
                renderer.render(lorebooksManager);

        assertEquals(
                List.of(
                        "History 1",
                        "Second depth section",
                        "First depth section",
                        "History 2"
                ),
                contents(result)
        );

        verify(chatHistorySection).injectEntriesAtDetectedOutlets(
                lorebooksManager,
                renderer
        );
    }

    private static PromptSection.InjectAtPosition.Relative relative(
            int number
    ) {
        return new PromptSection.InjectAtPosition.Relative(number);
    }

    private static PromptSection.InjectAtPosition.AtDepth atDepth(
            int depth
    ) {
        return new PromptSection.InjectAtPosition.AtDepth(depth);
    }

    private static ChatMessage chatMessage(String content) {
        ChatMessage message = mock(ChatMessage.class);

        when(message.asChatCompletion())
                .thenReturn(new ChatCompletionMessage(
                        ChatCompletionRole.USER,
                        null,
                        content
                ));

        return message;
    }

    private static List<String> contents(
            List<ChatCompletionMessage> messages
    ) {
        return messages.stream()
                .map(ChatCompletionMessage::content)
                .toList();
    }

    private static Result<Record2<Integer, String>> emptyOutlets() {
        return JOOQ.newResult(
                OUTLET.ID,
                OUTLET.OUTLET_
        );
    }
}