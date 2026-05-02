package chechelpo.frplm.frameworks.entities.assets;

import chechelpo.frplm.asset.AppDirectory;
import chechelpo.frplm.asset.AssetInfo;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.types.NotFound;
import chechelpo.frplm.exceptions.types.UnsupportedAction;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityService;
import chechelpo.frplm.utils.NameValidator;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.TableRecord;

import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>In charge of managing a Domain's entity asset folder, ensuring no collision between assets.</p>
 * The following path is enforced: 
 * <p>
 * "{@link AppDirectory#APP_DIRECTORY}/assets/{@link #entityName}/{@link AssetType}"
 * </p>
 * Also prevents fetching paths of non-existing entities via {@link ABSEntityService#exists(EntityKey)} and creating asset registries
 * with the same names.
 * @param <Key> ID object of the entity
 * @param <R> Table record related with the entity
 * @param <S> Service providing access to the entities
 */
public abstract class EntityAssetRegistry<
        R extends TableRecord<R>,
        S extends ABSEntityService<R,?>
        >
{
    private static final Set<String> ENTITIES_NAMES = new HashSet<>();

    private final Path root = AppDirectory.APP_DIRECTORY.resolve("assets");
    /**
     * A set of different asset types featured by this entity.
     */
    private final EnumSet<AssetType> types;
    private final String entityName;
    private final S service;

    protected EntityAssetRegistry(@NotNull S service, String entityName, @NotNull EnumSet<AssetType> types) {
        if (types.isEmpty()) throw new IllegalArgumentException("types cannot be empty");
        if (ENTITIES_NAMES.contains(entityName)) throw new IllegalStateException("Entity " + entityName + " is already registered");
        ENTITIES_NAMES.add(entityName);

        this.service = service;
        this.entityName = entityName;
        this.types = types;
    }

    public Set<AssetType> supportedTypes() {
        return Collections.unmodifiableSet(types);
    }

    /**
     * Function intended to allow representing weak entity relations within the folder's layout by injecting a prefix.
     * Meaning $ParentEntityPath/entityName/$WeakEntityPath
     * @param key of the object to fetch the path from
     * @return a prefix to the entity's folder ($App_directory/assets/<b>custom_prefix</b>/entityName/...).
     * Null if this registry is managing a strong entity
     */
    @Contract(pure = true)
    protected abstract @Nullable Path getPrefix(EntityKey<R> key);

    /**
     * @param assetType to search
     * @param id of entity
     * @throws NotFound if entity not registered in service.
     * @throws UnsupportedAction if requested asset type is not registered
     * @return the path to this asset type
     * @apiNote The path is either a file or a folder, depending if the {@link AssetInfo} marks it as multiple
     */
    public @NotNull Path getPathToAsset(@NotNull AssetType assetType, @NotNull EntityKey<R> id)
            throws NotFound, UnsupportedAction
    {
        if (!service.exists(id)) throw new NotFound(
                    "Entity of type " + entityName + " with id " + id + " not found when fetching asset type " + assetType,
                    Severity.EXPECTED
            );
        if (!types.contains(assetType)) throw new UnsupportedAction(
                "Asset type " + assetType + " does not exist for entity " + entityName
        );
        return getPathPrivate(id).resolve(assetType.getInfo().assetName());
    }

    /**
     * @param id of the entity
     * @return the path to this entity's folder
     * @throws NotFound if entity not registered in service
     */
    public @NotNull Path getPath(@NotNull EntityKey<R> id) throws NotFound{
        if (!service.exists(id)) throw new NotFound(
                "Entity of type " + entityName + " with id " + id + " not found when getting asset path"
                , Severity.EXPECTED
        );

        return getPathPrivate(id);
    }

    protected Path getPathPrivate(@NotNull EntityKey<R> id) {
        NameValidator.validOrThrow(id.toFolderName());

        Path prefix = getPrefix(id);
        if (prefix != null){
            return prefix.resolve(entityName).resolve(id.toFolderName());
        }

        return root.resolve(entityName).resolve(id.toFolderName());
    }
}
