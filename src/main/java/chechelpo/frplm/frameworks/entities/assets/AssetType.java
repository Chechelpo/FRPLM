package chechelpo.frplm.frameworks.entities.assets;

import chechelpo.frplm.asset.AssetInfo;
import org.jetbrains.annotations.NotNull;

public enum AssetType {
    AVATAR(new AssetInfo(false, "avatar")),
    BACKGROUNDS(new AssetInfo(true, "backgrounds")),
    EXPRESSIONS(new AssetInfo(true, "expressions")),
    ;
    private final AssetInfo info;

    AssetType(AssetInfo info) {
        this.info = info;
    }

    public AssetInfo getInfo() {
        return info;
    }

    public static AssetType ofName(@NotNull String name) {
        return AssetType.valueOf(name.toUpperCase().trim());
    }
}
