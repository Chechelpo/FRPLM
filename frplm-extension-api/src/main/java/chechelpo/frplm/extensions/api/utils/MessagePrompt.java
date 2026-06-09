package chechelpo.frplm.extensions.api.utils;

import chechelpo.frplm.extensions.api.standalone.EntrySnapshot;
import chechelpo.frplm.extensions.api.standalone.LorebookSnapshot;
import chechelpo.frplm.openai_compatible.ChatCompletionRequest;

public record MessagePrompt(
        LorebookSnapshot[] lorebooksUsed,
        EntrySnapshot[] injectedEntries,
        ChatCompletionRequest renderedRequest
) {
}
