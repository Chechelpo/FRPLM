package io.github.chechelpo.frplm.core.entities.assets;

import io.github.chechelpo.frplm.core.entities.fields.DTOMapper;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import org.jooq.TableRecord;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.HttpStatus.*;

public abstract class EntityAssetController<R extends TableRecord<R>> {
    protected final DTOMapper<R> mapper;
    protected final EntityReader<R> entityReader;
    protected final EntityAssetStore<R, ?> assetStore;

    protected EntityAssetController(EntityAssetStore<R, ?> assetStore, DTOMapper<R> mapper, EntityReader<R> reader) {
        this.mapper = mapper;
        this.entityReader = reader;
        this.assetStore = assetStore;
    }

    /**
     * Returns the actual asset contents.
     * Example:
     * GET /asset/portrait.webp?id=42
     */
    @GetMapping("/asset/{name}")
    public ResponseEntity<Resource> getAsset(
            @PathVariable String name,
            @RequestParam Map<String, String> keyParams
    ) {
        R record = findEntity(keyParams);

        Optional<EntityAssetStore.ReadableAsset> optionalAsset;
        try {
            optionalAsset = assetStore.findReadable(
                            record,
                            AssetTypes.requireValue(name)
                    );
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    INTERNAL_SERVER_ERROR,
                    "Could not read asset: " + name,
                    exception
            );
        }

        if (optionalAsset.isEmpty()) return ResponseEntity.noContent().build();

        EntityAssetStore.ReadableAsset asset = optionalAsset.get();
        MediaType mediaType = parseMediaType(asset.contentType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(asset.metadata().size())
                .lastModified(
                        asset.metadata()
                                .lastModified()
                                .toEpochMilli()
                )
                .cacheControl(
                        CacheControl.maxAge(Duration.ofHours(1))
                                .cachePrivate()
                )
                .body(asset.resource());
    }

    @GetMapping("/asset-metadata/{name}")
    public ResponseEntity<EntityAssetStore.StoredAsset> getAssetMetadata(
            @PathVariable String name,
            @RequestParam Map<String, String> keyParams
    ) {
        R record = findEntity(keyParams);

        try {
            return assetStore.find(
                            record,
                            AssetTypes.requireValue(name)
                    )
                    .map(ResponseEntity::ok)
                    .orElseThrow(() -> new EntityNotFound("No asset " + name + " for record \n" + record, Severity.EXPECTED));
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    INTERNAL_SERVER_ERROR,
                    "Could not read asset metadata: " + name,
                    exception
            );
        }
    }

    /**
     * Uploads or replaces an entity asset.
     * <p>
     * Example:
     * POST /asset/avatar?id=42&replace=true
     * </p>
     * Multipart field:
     * file=<uploaded file>
     */
    @PostMapping(
            value = "/asset/{name}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<EntityAssetStore.StoredAsset> uploadAsset(
            @PathVariable String name,
            @RequestPart("file") MultipartFile file,
            @RequestParam(
                    name = "replace",
                    defaultValue = "true"
            ) boolean replace,
            @RequestParam Map<String, String> requestParams
    ) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Uploaded asset must not be empty"
            );
        }

        AssetTypes assetType =
                AssetTypes.requireValue(name);

        /*
         * The generic request-parameter map also contains controller-specific
         * parameters. Remove them before constructing the entity key.
         */
        Map<String, String> keyParams =
                new HashMap<>(requestParams);

        keyParams.remove("replace");

        R record = findEntity(keyParams);

        boolean existedBefore =
                assetStore.exists(record, assetType);

        EntityAssetStore.WriteMode writeMode =
                replace
                        ? EntityAssetStore.WriteMode.REPLACE
                        : EntityAssetStore.WriteMode.CREATE_NEW;

        try (InputStream content = file.getInputStream()) {
            EntityAssetStore.StoredAsset storedAsset =
                    assetStore.save(
                            record,
                            assetType,
                            content,
                            writeMode
                    );

            return ResponseEntity
                    .status(existedBefore ? OK : CREATED)
                    .body(storedAsset);

        } catch (FileAlreadyExistsException exception) {
            throw new ResponseStatusException(
                    CONFLICT,
                    "Asset already exists: "
                            + assetType.wireValue(),
                    exception
            );

        } catch (IOException exception) {
            throw new ResponseStatusException(
                    INTERNAL_SERVER_ERROR,
                    "Could not save asset: "
                            + assetType.wireValue(),
                    exception
            );
        }
    }

    @DeleteMapping("/asset/{name}")
    public ResponseEntity<Void> deleteAsset(
            @PathVariable String name,
            @RequestParam Map<String, String> keyParams
    ) {
        R record = findEntity(keyParams);
        AssetTypes assetType = AssetTypes.requireValue(name);

        try {
            assetStore.delete(record, assetType);

            // Idempotent deletion.
            return ResponseEntity.noContent().build();
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    INTERNAL_SERVER_ERROR,
                    "Could not delete asset: " + assetType.wireValue(),
                    exception
            );
        }
    }

    private R findEntity(Map<String, String> requestParams) {
        /*
         * Convert because DTOMapper currently expects Map<String, Object>.
         */
        Map<String, Object> keyParams =
                new HashMap<>(requestParams);

        EntityKey<R> key = mapper.getKeyFromDTO(
                keyParams,
                DTOMapper.KEY_CONSTRUCTION_MODE.FULL_KEY
        );

        return entityReader.find(key).orElseThrow();
    }

    private static MediaType parseMediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
