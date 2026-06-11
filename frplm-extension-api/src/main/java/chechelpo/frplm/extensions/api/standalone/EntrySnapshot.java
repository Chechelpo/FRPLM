package chechelpo.frplm.extensions.api.standalone;

import chechelpo.frplm.extensions.api.annotations.Ephemeral;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@Ephemeral
public non-sealed interface EntrySnapshot extends Snapshot{
    record Reference(int lorebookId, int entryId) implements StableReference {
        private static final String prefix = "entry: ";

        @Override
        public @NotNull String encode() {
            return prefix + concat(Integer.toString(lorebookId), Integer.toString(entryId));
        }

        @Contract("_ -> new")
        public static @NotNull LocationSnapshot.Reference fromString(@NotNull String value) {
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

                return new LocationSnapshot.Reference(worldId, entryId);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Could not parse entry reference: " + value, e);
            }
        }
    }

}
