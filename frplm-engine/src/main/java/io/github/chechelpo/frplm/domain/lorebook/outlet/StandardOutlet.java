package io.github.chechelpo.frplm.domain.lorebook.outlet;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.interfaces.StableRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.OutletRecord;
import io.github.chechelpo.frplm.utils.macros.Macro;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.regex.Pattern;

import static io.github.chechelpo.frplm.jooq.generated.Tables.OUTLET;

/**
 * Contains standard outlets as well as outlet logic. An outlet is any macro in the form: "{{outlet:name}}"
 */
public enum StandardOutlet implements StableRecord<OutletRecord> {
    CHARACTER_INFO(1, "character_info"),
    LOCATION_INFO(2, "location_info"),
    WORLD_INFO(3, "world_info"),
    LOREBOOK(4, "lorebook");

    public final int stable_id;
    private EntityKey<OutletRecord> key;
    public final String name;
    private final Macro macro;

    StandardOutlet(int stable_id, @NotNull String name) {
        this.stable_id = stable_id;
        this.name = name;
        this.key = EntityKey.of(OUTLET.ID, stable_id);
        this.macro = asMacro(name);
    }

    public int getStableId() {
        return stable_id;
    }

    public Macro getMacro(){
        return this.macro;
    }

    @Contract(pure = true)
    public static @NotNull Macro asMacro(String outlet) {
        return new Macro("outlet:" + outlet);
    }



    @Contract(" -> new")
    @Override
    public @NotNull Optional<EntityDataPayload<OutletRecord>> toPayload() {
        return Optional.of(EntityDataPayload.<OutletRecord>builder()
                .set(OUTLET.ID, this.stable_id)
                .set(OUTLET.OUTLET_, this.name)
                .build()
        );
    }

    @Contract(" -> new")
    @Override
    public @NotNull Optional<EntityKey<OutletRecord>> toKey() {
        return Optional.of(key);
    }
}
