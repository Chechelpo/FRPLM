package chechelpo.frplm.extensions.api.session;

import chechelpo.frplm.extensions.api.annotations.Ephemeral;
import chechelpo.frplm.extensions.api.standalone.PromptSnapshot;
import chechelpo.frplm.extensions.api.standalone.Snapshot;
import chechelpo.frplm.extensions.api.standalone.StableReference;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.List;
import java.util.Optional;

@Ephemeral
public non-sealed interface Session extends Snapshot {
    record Reference(int id) implements StableReference {
        private static final String prefix = "session: ";

        @Contract(pure = true)
        @Override
        public @NotNull String encode() {
            return prefix + id;
        }

        @Contract("_ -> new")
        public static @NotNull Session.Reference fromString(@NotNull String value){
            if (!value.startsWith(prefix)) throw new IllegalArgumentException("Does not start with " + prefix);
            String raw = value.substring(prefix.length());

            try{
                return new Session.Reference(Integer.parseInt(raw));
            }catch(NumberFormatException e){
                throw new IllegalArgumentException("Does not parse " + raw);
            }
        }
    }
    Session.Reference reference();

    SessionCharacter getUserCharacter();
    SessionWorld getWorld();
    Optional<SessionPrompt> getPrompt();

    @UnmodifiableView
    List<ChatMessage> getChatHistory();
    List<ChatMessage> getLastMessages(int number);
    ChatMessage getLastMessage();

}
