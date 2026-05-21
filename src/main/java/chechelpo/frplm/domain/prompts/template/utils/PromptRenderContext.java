package chechelpo.frplm.domain.prompts.template.utils;

import chechelpo.frplm.domain.connection.llm.utils.generationRequest.ChatMessage;
import chechelpo.frplm.domain.lorebook.utils.Lorebook;
import chechelpo.frplm.domain.world.location.utils.LocationEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record PromptRenderContext(
        @NotNull LocationEntity currentLocation,
        @NotNull Lorebook[] lorebooks,
        @NotNull List<ChatMessage> messageHistory
) {}