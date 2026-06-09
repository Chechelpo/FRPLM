package chechelpo.frplm.extensions.api.session;

import chechelpo.frplm.extensions.api.standalone.PromptSnapshot;
import chechelpo.frplm.extensions.api.utils.MessagePrompt;
import chechelpo.frplm.openai_compatible.ChatCompletionRequest;

public interface SessionPrompt extends PromptSnapshot {
    MessagePrompt getNewMessagePrompt();

}
