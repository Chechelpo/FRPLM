package io.github.chechelpo.frplm.core.prompt.building;

import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRequest;

public record PromptResult(
        ChatCompletionRequest request,
        LorebooksManager lorebooksManager
) {
}
