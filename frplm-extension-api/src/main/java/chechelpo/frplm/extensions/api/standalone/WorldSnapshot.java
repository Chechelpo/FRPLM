package chechelpo.frplm.extensions.api.standalone;

import chechelpo.frplm.extensions.api.annotations.Ephemeral;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@Ephemeral
public non-sealed interface WorldSnapshot extends Snapshot {
    record Reference(int id) implements StableReference {
        private static final String prefix = "world: ";

        @Contract(pure = true)
        @Override
        public @NotNull String encode() {
            return prefix + id;
        }

        @Contract("_ -> new")
        public static @NotNull WorldSnapshot.Reference fromString(@NotNull String value){
            if (!value.startsWith(prefix)) throw new IllegalArgumentException("Does not start with " + prefix);
            String raw = value.substring(prefix.length());

            try{
                return new WorldSnapshot.Reference(Integer.parseInt(raw));
            }catch(NumberFormatException e){
                throw new IllegalArgumentException("Does not parse " + raw);
            }
        }
    }

    WorldSnapshot.Reference reference();
    LorebookSnapshot lorebook();
    LocationSnapshot[] getNeighboursOf(LocationSnapshot loc);
    boolean areNeighbours(LocationSnapshot loc, LocationSnapshot other);
}
