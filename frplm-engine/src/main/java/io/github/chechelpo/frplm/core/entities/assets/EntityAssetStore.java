package io.github.chechelpo.frplm.core.entities.assets;

import io.github.chechelpo.frplm.config.directories.AppDirectory;
import io.github.chechelpo.frplm.events.crud.CRUDCommittedEvent;
import io.github.chechelpo.frplm.extensions.api.standalone.StableReference;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.utils.IO.ZipReader;
import io.github.chechelpo.frplm.utils.converters.WebpImageConverter;
import io.github.chechelpo.frplm.utils.orders.CreationOrder;
import org.jooq.TableRecord;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public abstract class EntityAssetStore<
        R extends TableRecord<R>,
        S extends StableReference
        > {
    private static final String DEFAULT_CONTENT_TYPE =
            "application/octet-stream";

    protected final EntityConfigs.Types entityType;

    private final Path assetsDirectory;
    private final EnumSet<AssetTypes> allowedAssetTypes;

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // CONSTRUCTION
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    protected EntityAssetStore(
            EntityConfigs.Types type,
            AppDirectory directory,
            EnumSet<AssetTypes> allowedAssetTypes
    ) {
        this.entityType = Objects.requireNonNull(
                type,
                "type"
        );

        this.assetsDirectory = Objects.requireNonNull(
                        directory,
                        "directory"
                )
                .assets()
                .toAbsolutePath()
                .normalize();

        Objects.requireNonNull(
                allowedAssetTypes,
                "allowedAssetTypes"
        );

        if (allowedAssetTypes.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one asset type must be allowed"
            );
        }

        this.allowedAssetTypes =
                EnumSet.copyOf(allowedAssetTypes);
    }

    /**
     * Converts a database record into its stable reference.
     */
    protected abstract S getStableReference(R record);

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // CAPABILITIES
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Returns an immutable view of the asset types supported by this store.
     */
    public final Set<AssetTypes> allowedAssetTypes() {
        return Set.copyOf(allowedAssetTypes);
    }

    /**
     * Returns whether this store supports the given asset type.
     */
    public final boolean supports(
            AssetTypes type
    ) {
        Objects.requireNonNull(type, "type");

        return allowedAssetTypes.contains(type);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // CREATE
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public final void storeAssetFromCreationOrder(CreationOrder<R> order, R record, ZipReader reader){
        Optional<String> basePath = order.getZipPath();
        order.getMismatches(record);
        if (basePath.isEmpty()) return;

        for (AssetTypes registeredAssetType : this.allowedAssetTypes){
            String assetPath = basePath.get() + "/" + registeredAssetType.wireValue();
            if (!reader.containsFile(assetPath)) continue;

            try {
                save(record, registeredAssetType, reader.openFile(assetPath).orElseThrow(), WriteMode.CREATE_NEW);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    /**
     * Saves an asset.
     *
     * <p>Assets whose wire value ends in {@code .webp} are decoded and
     * re-encoded as valid WebP images before being stored.</p>
     *
     * <p>The caller retains ownership of {@code content} and must close it.</p>
     */
    public final StoredAsset save(
            R record,
            AssetTypes type,
            InputStream content,
            WriteMode mode
    ) throws IOException {
        Objects.requireNonNull(record, "record");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(mode, "mode");

        Path target = pathFor(record, type);
        Path parent = target.getParent();

        Files.createDirectories(parent);

        /*
         * Fail early when possible. The final move still enforces CREATE_NEW
         * atomically with respect to competing writers.
         */
        if (
                mode == WriteMode.CREATE_NEW
                        && Files.exists(
                        target,
                        LinkOption.NOFOLLOW_LINKS
                )
        ) {
            throw assetAlreadyExists(
                    target,
                    type
            );
        }

        Path stagedAsset =
                Files.createTempFile(
                        parent,
                        "." + target.getFileName() + "-",
                        ".staged"
                );

        try {
            writeStagedAsset(
                    type,
                    content,
                    stagedAsset
            );

            installStagedAsset(
                    stagedAsset,
                    target,
                    type,
                    mode
            );

            return metadata(
                    type,
                    target
            );
        } finally {
            /*
             * When installation succeeds, stagedAsset no longer exists.
             * When conversion or installation fails, remove the incomplete file.
             */
            Files.deleteIfExists(stagedAsset);
        }
    }

    /**
     * Returns whether uploaded content for this asset type must be normalized
     * into WebP.
     *
     * <p>Subclasses may override this when an asset has a WebP representation
     * but its wire value does not end in {@code .webp}.</p>
     */
    protected boolean requiresWebpConversion(
            AssetTypes type
    ) {
        return type.wireValue()
                .toLowerCase(Locale.ROOT)
                .endsWith(".webp");
    }

    private void writeStagedAsset(
            AssetTypes type,
            InputStream content,
            Path stagedAsset
    ) throws IOException {
        if (requiresWebpConversion(type)) {
            WebpImageConverter.convert(
                    content,
                    stagedAsset
            );

            return;
        }

        Files.copy(
                content,
                stagedAsset,
                REPLACE_EXISTING
        );
    }

    private static void installStagedAsset(
            Path stagedAsset,
            Path target,
            AssetTypes type,
            WriteMode mode
    ) throws IOException {
        switch (mode) {
            case CREATE_NEW ->
                    installNewAsset(
                            stagedAsset,
                            target,
                            type
                    );

            case REPLACE ->
                    replaceExistingAsset(
                            stagedAsset,
                            target
                    );
        }
    }

    private static void installNewAsset(
            Path stagedAsset,
            Path target,
            AssetTypes type
    ) throws IOException {
        try {
            /*
             * Omitting REPLACE_EXISTING preserves CREATE_NEW semantics.
             */
            Files.move(
                    stagedAsset,
                    target
            );
        } catch (FileAlreadyExistsException exception) {
            throw assetAlreadyExists(
                    target,
                    type
            );
        }
    }

    private static void replaceExistingAsset(
            Path stagedAsset,
            Path target
    ) throws IOException {
        try {
            /*
             * The staged file is created in the target directory, making an
             * atomic move possible on filesystems that support it.
             */
            Files.move(
                    stagedAsset,
                    target,
                    ATOMIC_MOVE,
                    REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(
                    stagedAsset,
                    target,
                    REPLACE_EXISTING
            );
        }
    }

    private static FileAlreadyExistsException assetAlreadyExists(
            Path target,
            AssetTypes type
    ) {
        return new FileAlreadyExistsException(
                target.toString(),
                null,
                "Asset already exists: "
                        + type.wireValue()
        );
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // READ
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Opens an existing asset.
     *
     * <p>The caller must close the returned stream.</p>
     */
    public final InputStream open(
            R record,
            AssetTypes type
    ) throws IOException {
        Path asset = requireRegularAsset(
                record,
                type
        );

        return Files.newInputStream(asset);
    }

    /**
     * Returns whether the entity has an asset of this type.
     */
    public final boolean exists(
            R record,
            AssetTypes type
    ) {
        Path asset = pathFor(record, type);

        return Files.isRegularFile(
                asset,
                LinkOption.NOFOLLOW_LINKS
        );
    }

    /**
     * Returns asset metadata when the asset exists.
     */
    public final Optional<StoredAsset> find(
            R record,
            AssetTypes type
    ) throws IOException {
        Path asset = pathFor(record, type);

        if (!Files.isRegularFile(
                asset,
                LinkOption.NOFOLLOW_LINKS
        )) {
            return Optional.empty();
        }

        return Optional.of(
                metadata(
                        type,
                        asset
                )
        );
    }

    /**
     * Returns an asset suitable for use in an HTTP response.
     */
    public final Optional<ReadableAsset> findReadable(
            R record,
            AssetTypes type
    ) throws IOException {
        Path asset = pathFor(record, type);

        if (!Files.isRegularFile(
                asset,
                LinkOption.NOFOLLOW_LINKS
        )) {
            return Optional.empty();
        }

        String contentType =
                Files.probeContentType(asset);

        if (contentType == null || contentType.isBlank()) {
            contentType = DEFAULT_CONTENT_TYPE;
        }

        return Optional.of(
                new ReadableAsset(
                        metadata(
                                type,
                                asset
                        ),
                        new FileSystemResource(asset),
                        contentType
                )
        );
    }

    /**
     * Lists all allowed single assets currently stored for the entity.
     */
    public final List<StoredAsset> list(
            R record
    ) throws IOException {
        Objects.requireNonNull(record, "record");

        List<StoredAsset> assets =
                new ArrayList<>();

        for (AssetTypes type : allowedAssetTypes) {
            if (type.isMultiple) {
                continue;
            }

            find(record, type)
                    .ifPresent(assets::add);
        }

        return List.copyOf(assets);
    }

    private Path requireRegularAsset(
            R record,
            AssetTypes type
    ) throws IOException {
        Path asset = pathFor(record, type);

        if (!Files.isRegularFile(
                asset,
                LinkOption.NOFOLLOW_LINKS
        )) {
            throw new NoSuchFileException(
                    asset.toString()
            );
        }

        return asset;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // DELETE
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Deletes one asset.
     *
     * @return {@code true} if the asset existed and was deleted
     */
    public final boolean delete(
            R record,
            AssetTypes type
    ) throws IOException {
        Path asset = pathFor(record, type);

        if (Files.isDirectory(
                asset,
                LinkOption.NOFOLLOW_LINKS
        )) {
            throw new IllegalStateException(
                    "Asset path unexpectedly refers to a directory: "
                            + type.wireValue()
            );
        }

        return Files.deleteIfExists(asset);
    }

    public final void deleteAll(
            R record
    ) throws IOException {
        Path entityDirectory = directoryFor(record);

        if (!Files.exists(
                entityDirectory,
                LinkOption.NOFOLLOW_LINKS
        )) {
            return;
        }

        List<Path> deletionOrder;

        try (Stream<Path> paths = Files.walk(entityDirectory)) {
            deletionOrder = paths
                    .sorted(Comparator.reverseOrder())
                    .toList();
        }

        for (Path path : deletionOrder) {
            try {
                Files.deleteIfExists(path);
            } catch (NoSuchFileException ignored) {
                // Concurrent deletion.
            }
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // UTILS
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /**
     * Returns the entity's asset directory when it exists.
     *
     * <p>The returned path is the physical source directory. Callers must not
     * modify or delete its contents.</p>
     */
    public final Optional<Path> findDirectory(
            R record
    ) throws IOException {
        Objects.requireNonNull(record, "record");

        Path directory = directoryFor(record);

        if (!Files.exists(
                directory,
                LinkOption.NOFOLLOW_LINKS
        )) {
            return Optional.empty();
        }

        if (!Files.isDirectory(
                directory,
                LinkOption.NOFOLLOW_LINKS
        )) {
            throw new IOException(
                    "Entity asset path is not a directory: "
                            + directory
            );
        }

        return Optional.of(directory);
    }
    /**
     * Returns the stable reference corresponding to the record.
     */
    protected final S referenceFor(
            R record
    ) {
        Objects.requireNonNull(record, "record");

        return Objects.requireNonNull(
                getStableReference(record),
                "getStableReference(record) returned null"
        );
    }

    /**
     * Returns the entity's asset directory:
     *
     * {@code {applicationRoot}/assets/{stableReference}}
     */
    protected final Path directoryFor(
            R record
    ) {
        String directoryName =
                validateDirectoryName(
                        referenceFor(record).encode()
                );

        return resolveWithin(
                assetsDirectory,
                Path.of(directoryName)
        );
    }

    /**
     * Returns the physical path for one declared asset type.
     */
    protected final Path pathFor(
            R record,
            AssetTypes type
    ) {
        requireAllowedAssetType(type);
        requireSingleAssetType(type);

        String assetName =
                validateAssetName(
                        type.wireValue()
                );

        return resolveWithin(
                directoryFor(record),
                Path.of(assetName)
        );
    }

    private void requireAllowedAssetType(
            AssetTypes type
    ) {
        Objects.requireNonNull(type, "type");

        if (!allowedAssetTypes.contains(type)) {
            throw new UnsupportedOperationException(
                    "Asset type "
                            + type
                            + " is not supported by "
                            + entityType
                            + ". Allowed asset types: "
                            + allowedAssetTypes
            );
        }
    }

    private static void requireSingleAssetType(
            AssetTypes type
    ) {
        if (type.isMultiple) {
            throw new UnsupportedOperationException(
                    "Multiple asset types require an indexed asset API: "
                            + type
            );
        }
    }

    private static StoredAsset metadata(
            AssetTypes type,
            Path asset
    ) throws IOException {
        return new StoredAsset(
                type,
                Files.size(asset),
                Files.getLastModifiedTime(
                        asset,
                        LinkOption.NOFOLLOW_LINKS
                ).toInstant()
        );
    }

    private static Path resolveWithin(
            Path parent,
            Path child
    ) {
        Path normalizedParent =
                parent.toAbsolutePath()
                        .normalize();

        Path resolved =
                normalizedParent.resolve(child)
                        .toAbsolutePath()
                        .normalize();

        if (!resolved.startsWith(normalizedParent)) {
            throw new IllegalArgumentException(
                    "Path escapes the permitted directory: "
                            + child
            );
        }

        return resolved;
    }

    private static String validateDirectoryName(
            String encodedReference
    ) {
        Objects.requireNonNull(
                encodedReference,
                "Encoded stable reference must not be null"
        );

        if (encodedReference.isBlank()) {
            throw new IllegalArgumentException(
                    "Encoded stable reference must not be blank"
            );
        }

        Path encodedPath =
                Path.of(encodedReference);

        if (
                encodedPath.isAbsolute()
                        || encodedPath.getNameCount() != 1
                        || encodedReference.equals(".")
                        || encodedReference.equals("..")
        ) {
            throw new IllegalArgumentException(
                    "Stable reference is not a valid directory name: "
                            + encodedReference
            );
        }

        return encodedReference;
    }

    private static String validateAssetName(
            String assetName
    ) {
        Objects.requireNonNull(
                assetName,
                "Asset wire value must not be null"
        );

        if (assetName.isBlank()) {
            throw new IllegalArgumentException(
                    "Asset wire value must not be blank"
            );
        }

        Path assetPath =
                Path.of(assetName);

        if (
                assetPath.isAbsolute()
                        || assetPath.getNameCount() != 1
                        || assetName.equals(".")
                        || assetName.equals("..")
        ) {
            throw new IllegalArgumentException(
                    "Asset wire value is not a valid file name: "
                            + assetName
            );
        }

        return assetName;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // TYPES
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public enum WriteMode {
        /**
         * Fails if the asset already exists.
         */
        CREATE_NEW,

        /**
         * Creates the asset or replaces its contents.
         */
        REPLACE
    }

    public record StoredAsset(
            AssetTypes type,
            long size,
            Instant lastModified
    ) {
        public StoredAsset {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(
                    lastModified,
                    "lastModified"
            );

            if (size < 0) {
                throw new IllegalArgumentException(
                        "Asset size cannot be negative"
                );
            }
        }
    }

    public record ReadableAsset(
            StoredAsset metadata,
            Resource resource,
            String contentType
    ) {
        public ReadableAsset {
            Objects.requireNonNull(metadata, "metadata");
            Objects.requireNonNull(resource, "resource");
            Objects.requireNonNull(
                    contentType,
                    "contentType"
            );
        }

        public AssetTypes type() {
            return metadata.type();
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Entity Deletion events
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @EventListener
    void entityDeleted(
            CRUDCommittedEvent.DeletedEntity<?> rawDeletedEntity
    ) {
        if (rawDeletedEntity.type() != entityType) {
            return;
        }

        @SuppressWarnings("unchecked")
        CRUDCommittedEvent.DeletedEntity<R> event =
                (CRUDCommittedEvent.DeletedEntity<R>) rawDeletedEntity;

        try {
            deleteAll(event.deletedRecord());
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Failed to delete assets for entity "
                            + event.key()
                            + " of type "
                            + entityType,
                    exception
            );
        }
    }
}
