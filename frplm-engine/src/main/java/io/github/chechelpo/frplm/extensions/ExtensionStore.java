package io.github.chechelpo.frplm.extensions;

import io.github.chechelpo.frplm.annotations.Store;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.JSON;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static chechelpo.frplm.jooq.generated.Tables.EXTENSION;

@Store
final class ExtensionStore {
    private final DSLContext ctx;
    private static final ObjectMapper mapper = new ObjectMapper();

    public ExtensionStore(DSLContext ctx){
        this.ctx = ctx;
    }

    public void createExtension(String extensionID, JsonNode defaultConfig){
        JSON value = null;
        if (defaultConfig != null)
            value = JSON.valueOf(defaultConfig.toString());

        this.ctx.insertInto(EXTENSION)
                .set(EXTENSION.ID, extensionID)
                .set(EXTENSION.CONFIG, value)
                .execute();
    }
    public void updateConfig(String extensionID, @NotNull JsonNode newConfig){
        this.ctx.update(EXTENSION)
                .set(EXTENSION.CONFIG, JSON.valueOf(newConfig.toString()))
                .where(EXTENSION.ID.eq(extensionID))
                .execute();
    }
    public JsonNode getConfig(String extensionID){
        JSON json = ctx.selectFrom(EXTENSION)
                .where(EXTENSION.ID.eq(extensionID))
                .fetchOne(EXTENSION.CONFIG);
        assert json != null;

        return mapper.readTree(json.data());
    }

    public boolean exists(String extensionID){
        return this.ctx.fetchExists(
                this.ctx.selectOne()
                        .from(EXTENSION)
                        .where(EXTENSION.ID.eq(extensionID))
        );
    }
}
