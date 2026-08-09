package io.github.chechelpo.frplm.domain.world.core;

import io.github.chechelpo.frplm.core.dispatch.readers.EntityReaders;
import io.github.chechelpo.frplm.core.entities.assets.EntityAssetStore;
import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.mappers.ABSWireMapper;
import io.github.chechelpo.frplm.core.entities.mappers.EntityWireMapper;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookJSON;
import io.github.chechelpo.frplm.domain.world.edge.EdgeJSON;
import io.github.chechelpo.frplm.domain.world.location.LocationJSON;
import io.github.chechelpo.frplm.domain.world.region.RegionJSON;
import io.github.chechelpo.frplm.jooq.generated.tables.records.*;
import io.github.chechelpo.frplm.utils.IO.ZipBuilder;
import io.github.chechelpo.frplm.utils.orders.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

@Component
public final class WorldMapper extends ABSWireMapper<WorldsRecord, WorldJSON, NewWorldOrder> {
    private final EntityReaders entityReaders;
    private final EntityWireMapper<LorebooksRecord, LorebookJSON, NewLorebookOrder> lorebookMapper;

    private final EntityWireMapper<RegionRecord, RegionJSON, NewRegionOrder> regionMapper;
    private final EntityWireMapper<LocationsRecord, LocationJSON, NewLocationOrder> locationMapper;
    private final EntityWireMapper<LocationEdgesRecord, EdgeJSON, NewEdgeOrder> edgeMapper;

    WorldMapper(
            ObjectMapper mapper,
            EntityReaders entityReaders,
            EntityWireMapper<LorebooksRecord, LorebookJSON, NewLorebookOrder> lorebookMapper,
            EntityWireMapper<LocationsRecord, LocationJSON, NewLocationOrder> locationMapper,
            EntityWireMapper<RegionRecord, RegionJSON, NewRegionOrder> regionMapper,
            EntityWireMapper<LocationEdgesRecord, EdgeJSON, NewEdgeOrder> edgeMapper,
            EntityAssetStore<WorldsRecord, ?> entityAssetStore
    ) {
        super(mapper, WorldJSON.class, entityAssetStore);

        this.entityReaders = entityReaders;
        this.lorebookMapper = lorebookMapper;

        this.regionMapper = regionMapper;
        this.locationMapper = locationMapper;
        this.edgeMapper = edgeMapper;
    }

    @Contract(pure = true)
    @Override
    @NonNull
    protected String getZipPath(@NonNull WorldJSON json) {
        return "world/" + json.name();
    }

    @Override
    protected WorldJSON internalRecordFrom(
            @NonNull WorldsRecord worldRecord,
            @NonNull ZipBuilder builder
    ) throws IOException {
        return new WorldJSON(
                worldRecord.getName(),
                worldRecord.getDescription(),

                // Background placement.
                worldRecord.getBackgroundX(),
                worldRecord.getBackgroundY(),
                worldRecord.getBackgroundWidth(),
                worldRecord.getBackgroundHeight(),

                // Background rendering/editing.
                worldRecord.getBackgroundOpacity(),
                worldRecord.getBackgroundVisible(),
                worldRecord.getBackgroundTransformLocked(),
                worldRecord.getBackgroundAspectLocked(),
                worldRecord.getBackgroundFit(),

                lorebookMapper.jsonRecordFrom(
                        entityReaders.lorebooks().require(
                                EntityKey.of(
                                        LOREBOOKS.ID,
                                        worldRecord.getLorebookId()
                                )
                        ),
                        builder
                ),

                getRegionsOfWorld(worldRecord, builder),
                getLocationsOfWorld(worldRecord, builder),
                getEdgesOfWorld(worldRecord, builder)
        );
    }

    private @NonNull @Unmodifiable List<LocationJSON> getLocationsOfWorld(
            @NonNull WorldsRecord worldsRecord,
            @NonNull ZipBuilder builder
    ) {
        return locationMapper.jsonRecordsFrom(
                entityReaders.locations().getMatching(
                        EntityDataPayload.of(
                                LOCATIONS.WORLD_ID,
                                worldsRecord.getId()
                        )
                ),
                builder
        );
    }

    private List<RegionJSON> getRegionsOfWorld(
            @NonNull WorldsRecord record,
            @NonNull ZipBuilder builder
    ) {
        return regionMapper.jsonRecordsFrom(
                entityReaders.regions().getMatching(
                        EntityDataPayload.of(
                                REGION.WORLD_ID,
                                record.getId()
                        )
                ),
                builder
        );
    }

    private List<EdgeJSON> getEdgesOfWorld(
            @NonNull WorldsRecord worldsRecord,
            @NonNull ZipBuilder builder
    ) {
        return edgeMapper.jsonRecordsFrom(
                entityReaders.edges().getMatching(
                        EntityDataPayload.of(
                                LOCATION_EDGES.WORLD_ID,
                                worldsRecord.getId()
                        )
                ),
                builder
        );
    }

    @Override
    protected NewWorldOrder internalOrderFrom(@NonNull WorldJSON json) {
        return new NewWorldOrder(
                getZipPath(json),

                EntityDataPayload.<WorldsRecord>builder()
                        .set(WORLDS.NAME, json.name())
                        .set(WORLDS.DESCRIPTION, json.description())

                        // Background placement.
                        .set(WORLDS.BACKGROUND_X, json.backgroundX())
                        .set(WORLDS.BACKGROUND_Y, json.backgroundY())
                        .set(WORLDS.BACKGROUND_WIDTH, json.backgroundWidth())
                        .set(WORLDS.BACKGROUND_HEIGHT, json.backgroundHeight())

                        // Background rendering/editing.
                        .set(WORLDS.BACKGROUND_OPACITY, json.backgroundOpacity())
                        .set(WORLDS.BACKGROUND_VISIBLE, json.backgroundVisible())
                        .set(WORLDS.BACKGROUND_TRANSFORM_LOCKED, json.backgroundTransformLocked())
                        .set(WORLDS.BACKGROUND_ASPECT_LOCKED, json.backgroundAspectLocked())
                        .set(WORLDS.BACKGROUND_FIT, json.backgroundFit())

                        .build(),

                lorebookMapper.orderFrom(json.lorebook()),
                locationMapper.ordersFrom(json.locations()),
                regionMapper.ordersFrom(json.regions()),
                edgeMapper.ordersFrom(json.edges())
        );
    }
}