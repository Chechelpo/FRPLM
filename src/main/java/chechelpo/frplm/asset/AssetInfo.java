package chechelpo.frplm.asset;

/**
 * @param multiple whether an entity can have more than one assigned
 * @param assetName asset folder (if multiple) or name of asset
 */
public record AssetInfo(
        boolean multiple,
        String assetName
) {}
