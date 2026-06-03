package chechelpo.frplm.extensions.api.session;

import chechelpo.frplm.openai_compatible.ChatCompletionMessage;

public interface ChatMessage {
    int getMessageNumber();
    SessionLocation getLocation();

    ChatCompletionMessage asChatCompletion();
}
