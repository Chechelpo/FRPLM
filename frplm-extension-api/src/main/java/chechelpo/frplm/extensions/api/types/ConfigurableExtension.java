package chechelpo.frplm.extensions.api.types;

import chechelpo.frplm.extensions.api.EngineRepository;
import chechelpo.frplm.extensions.api.utils.ExtensionDBBridge;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import tools.jackson.databind.JsonNode;

public abstract class ConfigurableExtension extends Extension {
    private final JsonNode defaultConfig;
    private ExtensionDBBridge dbBridge;

    protected ConfigurableExtension(
            String extensionID,
            String name,
            String description,
            String source,
            JsonNode defaultConfig
    ) {
        super(extensionID, name, description, source);
        this.defaultConfig = defaultConfig;
    }

    public abstract void onConfigChange(JsonNode newConfig);
    public final void setDBBridge(ExtensionDBBridge extensionDBBridge) {
        this.dbBridge = extensionDBBridge;
    }

    protected final JsonNode getCurrentConfig(){
        return this.dbBridge.getConfig(this.extensionId());
    }
    protected final void saveConfig(JsonNode config) {
        this.dbBridge.saveConfig(this.extensionId(), config);
    }

    /**
     * URL that the frontend can render as the extension's config panel.
     */
    public @NotNull String configPanelUrl(){
        return getConfigPanelURL(this);
    }

    @Override
    public final JsonNode defaultConfig() {
        return this.defaultConfig;
    }

    @Contract(pure = true)
    public static @NotNull String getConfigPanelURL(@NotNull ConfigurableExtension extension){
        return "/api/extensions/"+extension.extensionId()+"/config-panel";
    }
}