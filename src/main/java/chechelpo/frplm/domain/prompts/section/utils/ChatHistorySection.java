package chechelpo.frplm.domain.prompts.section.utils;

import chechelpo.frplm.domain.connection.llm.utils.generationRequest.ChatMessage;
import chechelpo.frplm.domain.lorebook.entry.utils.Entry;
import chechelpo.frplm.domain.prompts.template.utils.Prompt;
import chechelpo.frplm.domain.prompts.template.utils.PromptRenderContext;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ChatHistorySection implements PromptSection {
    List<ChatMessage> messages;
    public ChatHistorySection(List<ChatMessage> messages) {
        this.messages = messages;
    }
    public void addMessage(@NotNull ChatMessage message) {}




    @Override
    public @NotNull List<ChatMessage> render(@NotNull PromptRenderContext context) {
        return List.of();
    }

    @Override
    public short position() {
        return 0;
    }

    @Override
    public boolean active() {
        return false;
    }

    @Override
    public @NotNull IntSet getDetectedOutlets() {
        return null;
    }

    @Override
    public @NotNull IntSet getDetectedKeywords() {
        return null;
    }

    @Override
    public void inject(@NotNull Int2ObjectMap<Entry> entries) {

    }
}
