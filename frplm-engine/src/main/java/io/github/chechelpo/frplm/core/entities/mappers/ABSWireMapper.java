package io.github.chechelpo.frplm.core.entities.mappers;

import io.github.chechelpo.frplm.core.entities.assets.EntityAssetStore;
import io.github.chechelpo.frplm.utils.IO.ZipBuilder;
import io.github.chechelpo.frplm.utils.IO.ZipReader;
import io.github.chechelpo.frplm.utils.orders.CreationOrder;
import org.jetbrains.annotations.Nullable;
import org.jooq.TableRecord;
import org.jspecify.annotations.NonNull;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public abstract non-sealed class ABSWireMapper<R extends TableRecord<R>, J, O extends CreationOrder<R>> implements EntityWireMapper<R, J, O> {
    protected final ObjectMapper mapper;
    protected final Class<J> jsonClazz;
    protected final EntityAssetStore<R, ?> assetStore;

    protected ABSWireMapper(
            ObjectMapper mapper,
            Class<J> jsonClazz,
            @Nullable EntityAssetStore<R, ?> assetStore
    ) {
        this.mapper = mapper;
        this.jsonClazz = jsonClazz;
        this.assetStore = assetStore;
    }

    protected abstract J internalRecordFrom(@NonNull R record, @NonNull ZipBuilder zipBuilder) throws IOException;

    protected abstract O internalOrderFrom(J json);

    protected abstract String getZipPath(J json);

    /** Returns this entity's json record and adds any assets this entity holds */
    public final JsonNode jsonFrom(R record, ZipBuilder builder) {
        try {
            J jsonRecord = internalRecordFrom(record, builder);
            addFiles(builder, record, jsonRecord);
            return mapper.valueToTree(jsonRecord);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public final J jsonRecordFrom(R record, ZipBuilder builder) {
        try{
            J jsonRecord = internalRecordFrom(record, builder);
            addFiles(builder, record, jsonRecord);
            return jsonRecord;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public final O orderFrom(J node) {
        Objects.requireNonNull(node);
        return internalOrderFrom(node);
    }


    protected final Optional<Path> getAssetDirectory(@NonNull R record) throws IOException {
        if (assetStore == null) return Optional.empty();
        return assetStore.findDirectory(record);
    }

    public final void addFiles(
            ZipBuilder zip,
            @NonNull R record,
            @NonNull J json
    ) throws IOException {
        Optional<Path> directory = getAssetDirectory(record);

        if (directory.isPresent()) {
            zip.addDirectory(
                    directory.get(),
                    getZipPath(json)
            );
        }
    }
}
