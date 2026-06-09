package chechelpo.frplm.extensions.api.standalone;

import chechelpo.frplm.extensions.api.annotations.Ephemeral;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@Ephemeral
public non-sealed interface LocationSnapshot extends Snapshot {
    record Reference(int worldId, int entryId) implements StableReference {
        private static final String prefix = "entry: ";
        @Contract(pure = true)
        @Override
        public @NotNull String encode() {
            return prefix + worldId + "," + entryId;
        }


        @Contract("_ -> new")
        public static @NotNull Reference fromString(@NotNull String value) {
            if (!value.startsWith(prefix)) {
                throw new IllegalArgumentException("Does not start with " + prefix);
            }

            String raw = value.substring(prefix.length());
            String[] parts = raw.split(",", -1);

            if (parts.length != 2) {
                throw new IllegalArgumentException("Expected format " + prefix + "<worldId>,<entryId>");
            }

            try {
                int worldId = Integer.parseInt(parts[0].trim());
                int entryId = Integer.parseInt(parts[1].trim());

                return new Reference(worldId, entryId);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Could not parse entry reference: " + value, e);
            }
        }
    }

    LocationSnapshot.Reference reference();
    LorebookSnapshot lorebook();
    LocationSnapshot[] getNeighbours();
    String getName();
}
