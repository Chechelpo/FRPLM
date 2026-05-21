package chechelpo.frplm.domain.lorebook.entry;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum ActivationStrategy {
    CONSTANT((short) 0),
    COMMON((short) 1),
    EMBEDDING((short) 2)
    ;
    public final short stable_id;

    ActivationStrategy(short stable_id) {
        this.stable_id = stable_id;
    }

    @Contract(value = " -> new", pure = true)
    public static int @NotNull [] stableIDs(){
        return new int[]{CONSTANT.stable_id, COMMON.stable_id, EMBEDDING.stable_id};
    }
    public static @NotNull ActivationStrategy of(short stable_id) {
        for (ActivationStrategy activationStrategy : ActivationStrategy.values()) {
            if (activationStrategy.stable_id == stable_id) return activationStrategy;
        }
        throw new IllegalArgumentException("No activation strategy found for stable_id: " + stable_id);
    }
}
