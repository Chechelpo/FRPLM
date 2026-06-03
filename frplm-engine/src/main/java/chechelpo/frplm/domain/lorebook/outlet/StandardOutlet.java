package chechelpo.frplm.domain.lorebook.outlet;

import chechelpo.frplm.frameworks.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityKey;
import chechelpo.frplm.interfaces.StableRecord;
import chechelpo.frplm.jooq.generated.tables.records.OutletRecord;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.regex.Pattern;

import static chechelpo.frplm.jooq.generated.Tables.OUTLET;

public enum StandardOutlet implements StableRecord<OutletRecord> {
    CHARACTER_INFO(1, "character_info"),
    LOCATION_INFO(2, "location_info"),
    WORLD_INFO(3, "world_info"),
    LOREBOOK(4, "lorebook"),
    CHAT_HISTORY(5, "chat_history")
    ;
    public static final Pattern UNRESOLVED_MACRO_LINE = Pattern.compile(
            "(?m)^[ \\t]*\\{\\{\\s*[^{}\\r\\n]+\\s*}}[ \\t]*(?:\\R|$)"
    );
    public static final Pattern UNRESOLVED_MACRO_INLINE = Pattern.compile(
            "\\{\\{\\s*[^{}\\r\\n]+\\s*}}"
    );

    public final int stable_id;
    private EntityKey<OutletRecord> key;
    public final String name;
    private final Pattern pattern;

    StandardOutlet(int stable_id, @NotNull String name) {
        this.stable_id = stable_id;
        this.name = name;
        this.key = EntityKey.of(OUTLET.ID, stable_id);
        this.pattern = asPattern(name);
    }

    @Contract(pure = true)
    public static @NotNull String asMacro(String outlet){
        return "{{"+outlet+"}}";
    }
    @Contract(pure = true)
    public static @NotNull Pattern asPattern(String outlet){
        return Pattern.compile(Pattern.quote(asMacro(outlet)), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    @Contract(pure = true)
    public @NotNull String asMacro(){
        return asMacro(this.name);
    }

    @Contract(pure=true)
    public @NotNull Pattern asPattern(){
        return this.pattern;
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
