package chechelpo.frplm.extensions.api.standalone;

import chechelpo.frplm.extensions.api.annotations.Ephemeral;
import chechelpo.frplm.openai_compatible.ChatCompletionRequest;
import chechelpo.frplm.openai_compatible.ChatCompletionResponse;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@Ephemeral
public interface ConnectionSnapshot {
    record Reference(int id) implements StableReference {
        private static final String prefix = "connection: ";

        @Contract(pure = true)
        @Override
        public @NotNull String encode() {
            return prefix + id;
        }

        @Contract("_ -> new")
        public static @NotNull ConnectionSnapshot.Reference fromString(@NotNull String value){
            if (!value.startsWith(prefix)) throw new IllegalArgumentException("Does not start with " + prefix);
            String raw = value.substring(prefix.length());

            try{
                return new ConnectionSnapshot.Reference(Integer.parseInt(raw));
            }catch(NumberFormatException e){
                throw new IllegalArgumentException("Does not parse " + raw);
            }
        }
    }
    ConnectionSnapshot.Reference reference();

    boolean hasApiKey();

    String getName();
    ChatCompletionResponse generate(ChatCompletionRequest request);
    String getModelID();
}
