package chechelpo.frplm.domain.prompts.section.utils;

import chechelpo.frplm.domain.connection.llm.utils.generationRequest.ChatMessage;
import chechelpo.frplm.domain.lorebook.entry.utils.Entry;
import chechelpo.frplm.domain.prompts.template.utils.PromptRenderContext;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public sealed interface PromptSection permits EntityPromptSection, ChatHistorySection {
    @NotNull List<ChatMessage> render(@NotNull PromptRenderContext context);

    short position();

    boolean active();

    @NotNull IntSet getDetectedOutlets();
    @NotNull IntSet getDetectedKeywords();
    void inject(@NotNull Int2ObjectMap<Entry> entries);
}