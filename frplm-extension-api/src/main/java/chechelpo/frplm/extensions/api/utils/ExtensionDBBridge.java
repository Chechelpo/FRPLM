package chechelpo.frplm.extensions.api.utils;

import tools.jackson.databind.JsonNode;

public interface ExtensionDBBridge {
    void saveConfig(String extensionID, JsonNode config);
    JsonNode getConfig(String extensionID);
}
