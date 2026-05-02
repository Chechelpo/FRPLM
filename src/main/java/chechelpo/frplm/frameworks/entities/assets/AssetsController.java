package chechelpo.frplm.frameworks.entities.assets;

import chechelpo.frplm.frameworks.entities.microservices.ABSEntityService;
import org.jooq.TableRecord;

import java.util.EnumMap;

public abstract class AssetsController<R extends TableRecord<R>, S extends ABSEntityService<R, ?>> {
    private final EnumMap<AssetType, AssetManager> assetManagers = new EnumMap<>(AssetType.class);
    protected final EntityAssetRegistry<R, S> registry;

    private final S service;
    public AssetsController(EntityAssetRegistry<R, S> registry, S service) {
        this.registry = registry;
        this.service = service;
    }
/*
    public void register_asset(@NotNull AssetManager manager){
        if (assetManagers.containsKey(manager.getType()))
            throw new IllegalArgumentException("Duplicate asset manager: " + manager.getType());

        assetManagers.put(manager.getType(), manager);
    }

    @PostMapping(
            value = "/{name}/{number}",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<byte[]> getAsset(
            @PathVariable("name") String assetName,
            @PathVariable("number") int number,
            @RequestBody Map<String, Object> keyParams
    ) throws IOException {
        EntityKey<R> key = service.keyOf(keyParams);
        AssetType assetType = validateOrder(assetName, key);

        return ResponseEntity.ok(
                assetManagers.get(assetType).getAssetsOfEntity(key, number)
        );
    }

    @PutMapping(
            value = "/{name}/{number}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Boolean> uploadAsset(
            @PathVariable("name") String assetName,
            @PathVariable("number") int number,
            @RequestPart("identifier") Map<String, Object> keyParams,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        if (file.isEmpty()) return ResponseEntity.badRequest().body(false);
        EntityKey<R> key = fields.keyOf(keyParams);
        AssetType assetType = validateOrder(assetName, key);

        return ResponseEntity.ok(
                assetManagers.get(assetType).uploadAsset(key, file, number)
        );
    }


    protected AssetType validateOrder(@NotNull String name, EntityKey<R> key) throws UnknownAsset, NotFound {
        AssetType assetType;
        try{
            assetType = AssetType.ofName(name);
        } catch (IllegalArgumentException e) {
            throw new UnknownAsset("No such asset type: " + name);
        }
        if (!assetManagers.containsKey(assetType))
            throw new UnknownAsset("No such asset type: " + assetType + " for entity type");

        service.require(key);
        return assetType;
    }*/
}
