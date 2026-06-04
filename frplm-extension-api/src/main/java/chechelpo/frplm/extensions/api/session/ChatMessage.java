package chechelpo.frplm.extensions.api.session;

import chechelpo.frplm.extensions.api.standalone.Snapshot;
import chechelpo.frplm.openai_compatible.ChatCompletionMessage;

public non-sealed interface ChatMessage extends Snapshot {
    int getMessageNumber();
    SessionLocation getLocation();

    ChatCompletionMessage asChatCompletion();
}
