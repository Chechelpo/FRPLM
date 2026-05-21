package chechelpo.frplm.domain.lorebook.outlet;

import org.jetbrains.annotations.NotNull;

public enum StandardOutlet {
    CHARACTER_INFO(1, "{{character_info}}"),
    LOCATION_INFO(2, "{{location_info}}"),
    WORLD_INFO(3, "{{world_info}}"),
    LOREBOOK(4, "{{lorebook}}"),
    CHAT_HISTORY(5, "{{chat_history}}"),
    ;
    public final int stable_id;
    public final String name;

    StandardOutlet(int stable_id, @NotNull String name) {
        this.stable_id = stable_id;
        this.name = name;
    }
}
