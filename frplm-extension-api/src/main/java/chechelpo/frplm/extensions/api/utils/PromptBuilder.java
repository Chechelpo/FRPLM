package chechelpo.frplm.extensions.api.utils;

import chechelpo.frplm.extensions.api.session.ChatMessage;
import chechelpo.frplm.extensions.api.standalone.LorebookSnapshot;
import chechelpo.frplm.openai_compatible.ChatCompletionMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.List;

public interface PromptBuilder {
    @UnmodifiableView
    List<LorebookSnapshot> getLorebooks();

    PromptBuilder addLorebook(LorebookSnapshot lorebook);

    PromptBuilder addLorebooks(List<LorebookSnapshot> lorebooks);

    PromptBuilder addLorebooks(LorebookSnapshot... lorebookSnapshots);

    /**
     * Appends a section-like message.
     *
     * <p>Unlike normal chat messages, appended sections participate in outlet
     * detection and lorebook entry injection during engine rendering.</p>
     */
    PromptBuilder appendAsSection(ChatCompletionMessage section);

    /**
     * Appends a normal chat message.
     *
     * <p>Normal chat messages are included in the final request, but are not
     * treated as prompt sections for outlet injection.</p>
     */
    PromptBuilder append(@NotNull ChatMessage message);

    PromptBuilder appendAll(@NotNull List<ChatMessage> chatHistory);

    PromptBuilder insertAt(int depth, ChatCompletionMessage message);
}