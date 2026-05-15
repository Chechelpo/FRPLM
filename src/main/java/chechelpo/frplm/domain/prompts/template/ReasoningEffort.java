package chechelpo.frplm.domain.prompts.template;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum ReasoningEffort {
    None(0, "none"),
    Minimal(1,"minimal"),
    Low(2,"low"),
    Medium(3,"medium"),
    High(4,"high"),
    Maximum(5,"xhigh")
    ;
    public final String value;
    public final int id;
    ReasoningEffort(int id, String value) {
        this.value = value;
        this.id = id;
    }

    @Contract(value = " -> new", pure = true)
    public static int @NotNull [] possible_values() {
        return new int[]{None.id, Minimal.id, Low.id, Medium.id, High.id, Maximum.id };
    }
    public static @NotNull ReasoningEffort fromId(int id) {
        for (ReasoningEffort reasoningEffort : values()) {
            if (reasoningEffort.id == id) {
                return reasoningEffort;
            }
        }
        throw new IllegalArgumentException("Invalid reasoning effort: " + id);
    }
}