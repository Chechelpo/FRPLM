package chechelpo.frplm.extensions.api.session;

import chechelpo.frplm.extensions.api.standalone.Snapshot;
import chechelpo.frplm.extensions.api.utils.DetectedOutlet;
import chechelpo.frplm.openai_compatible.ChatCompletionMessage;

import java.util.List;

public non-sealed interface ChatMessage extends Snapshot {
    int getMessageNumber();
    SessionLocation getLocation();
    String content();
    ChatCompletionMessage asChatCompletion();
    List<DetectedOutlet> getDetectedOutlets();
}
