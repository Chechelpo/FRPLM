package chechelpo.frplm.extensions.api.standalone;

import chechelpo.frplm.extensions.api.annotations.Ephemeral;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Ephemeral
public non-sealed interface PromptSnapshot extends Snapshot {
    record Reference(int id) implements StableReference{
        private static final String prefix = "prompt: ";

        @Contract(pure = true)
        @Override
        public @NotNull String encode() {
            return prefix + id;
        }

        @Contract("_ -> new")
        public static @NotNull PromptSnapshot.Reference fromString(@NotNull String value){
            if (!value.startsWith(prefix)) throw new IllegalArgumentException("Does not start with " + prefix);
            String raw = value.substring(prefix.length());

            try{
                return new PromptSnapshot.Reference(Integer.parseInt(raw));
            }catch(NumberFormatException e){
                throw new IllegalArgumentException("Does not parse " + raw);
            }
        }
    }

    PromptSnapshot.Reference reference();
    Optional<ConnectionSnapshot> getAssignedConnection();
}
