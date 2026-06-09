package chechelpo.frplm.extensions.api.standalone;

import chechelpo.frplm.extensions.api.annotations.Ephemeral;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@Ephemeral
public non-sealed interface CharacterSnapshot extends Snapshot {
    record Reference(int id) implements chechelpo.frplm.extensions.api.standalone.StableReference {
        private static final String prefix = "character: ";
        @Override
        public @NotNull String encode() {
            return prefix + id;
        }

        @Contract("_ -> new")
        public static @NotNull CharacterSnapshot.Reference fromString(@NotNull String value){
            if (!value.startsWith(prefix)) throw new IllegalArgumentException("Does not start with " + prefix);
            String raw = value.substring(prefix.length());

            try{
                return new Reference(Integer.parseInt(raw));
            }catch(NumberFormatException e){
                throw new IllegalArgumentException("Does not parse " + raw);
            }
        }
    }

    CharacterSnapshot.Reference reference();
    LorebookSnapshot lorebook();
    String getName();
    LocationSnapshot[] getStartingLocations();
}
