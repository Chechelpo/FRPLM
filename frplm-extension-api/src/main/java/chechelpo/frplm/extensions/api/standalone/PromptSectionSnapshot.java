package chechelpo.frplm.extensions.api.standalone;


import chechelpo.frplm.openai_compatible.ChatCompletionMessage;
import chechelpo.frplm.openai_compatible.ChatCompletionRole;

public non-sealed interface PromptSectionSnapshot extends Snapshot {
    public record Reference(int promptId, int sectionId) implements StableReference {
        private static final String prefix = "section: ";
        @Override
        public String encode() {
            return prefix + concat(Integer.toString(promptId), Integer.toString(sectionId));
        }
    }
    Reference reference();
    String content();
    ChatCompletionMessage asCompletionMessage();
    ChatCompletionRole role();
}
