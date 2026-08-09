package io.github.chechelpo.frplm.extensions.implementations.standalone;

import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.extensions.api.standalone.LocationSnapshot;
import io.github.chechelpo.frplm.extensions.api.standalone.LorebookSnapshot;
import io.github.chechelpo.frplm.extensions.api.standalone.RegionSnapshot;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationEdgesRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

public class LocationImpl extends StandaloneEntity<LocationsRecord> implements LocationSnapshot {
    public LocationImpl(LocationsRecord record, ExtensionContext context) {
        super(record, context);
    }

    public LocationsRecord getRecord() {
        return record;
    }

    @Override
    public Reference asReference() {
        return new LocationSnapshot.Reference(this.record.getWorldId(), this.record.getId());
    }

    @Override
    public String getName() {
        return record.getName();
    }

    @Override
    public String getDescription() {
        return record.getDescription();
    }

    @Override
    public LocationImpl @NotNull [] getNeighbours() {
        return context.edges().neighboursOf(this.record).stream()
                .map(record -> new LocationImpl(record, this.context))
                .toArray(LocationImpl[]::new);
    }

    @Override
    public List<Edge<LocationSnapshot>> getOutEdges() {
        return context.edges().getMatching(
                        EntityKey.<LocationEdgesRecord>builder()
                                .set(LOCATION_EDGES.WORLD_ID, record.getWorldId())
                                .set(LOCATION_EDGES.FROM_LOCATION_ID, record.getId())
                                .build()
                ).stream()
                .map(record ->
                        new Edge<LocationSnapshot>(
                                new LocationImpl(
                                        context.locations().find(
                                                EntityKey.<LocationsRecord>builder()
                                                        .set(LOCATIONS.WORLD_ID, record.getWorldId())
                                                        .set(LOCATIONS.ID, record.getToLocationId())
                                                        .build()
                                        ).orElseThrow(
                                                "Somehow there's an edge in the DB that locations service can't find. Go ape shit right now",
                                                Severity.SYSTEM
                                        ),
                                        context
                                ),
                                record.getEdgedescription(),
                                record.getTraversable(),
                                record.getShowDestinationName(),
                                record.getShowDestinationDescription()
                        )
                )
                .toList();
    }

    @Override
    public RegionSnapshot getParentRegion() {
        if (record.getRegionId() == null)
            throw new IllegalStateException("This location has no parent region");
        return new RegionImpl(
                context.regions().find(
                                EntityKey.<RegionRecord>builder()
                                        .set(REGION.WORLD_ID, record.getWorldId())
                                        .set(REGION.ID, record.getRegionId())
                                        .build()
                        )
                        .orElseThrow(notFound -> new EntityNotFound(
                                "This location holds a stale parent region " + notFound.toString(),
                                Severity.SYSTEM)
                        ),
                context
        );
    }

    @Override
    public LorebookSnapshot lorebook() {
        return new LorebookImpl(context.lorebooks().getLorebookOf(this.getRecord()), context);
    }
}
