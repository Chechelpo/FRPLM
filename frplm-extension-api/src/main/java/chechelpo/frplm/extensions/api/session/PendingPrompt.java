package chechelpo.frplm.extensions.api.session;

import chechelpo.frplm.extensions.api.standalone.LorebookSnapshot;
import chechelpo.frplm.openai_compatible.ChatCompletionMessage;

public interface PendingPrompt {
    boolean addSection(ChatCompletionMessage message, int atDepth);
    LorebookSnapshot[] getLorebooks();
}
