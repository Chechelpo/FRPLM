package io.github.chechelpo.frplm.extensions.implementations.standalone;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.exceptions.runtime.UnexpectedException;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.extensions.api.standalone.LocationSnapshot;
import io.github.chechelpo.frplm.extensions.api.standalone.LorebookSnapshot;
import io.github.chechelpo.frplm.extensions.api.standalone.RegionSnapshot;
import io.github.chechelpo.frplm.extensions.api.standalone.WorldSnapshot;

import java.util.List;
import java.util.Optional;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

public class RegionImpl extends StandaloneEntity<RegionRecord> implements RegionSnapshot {
    public RegionImpl(RegionRecord record, ExtensionContext context) {
        super(record, context);
    }

    @Override
    public RegionRecord getRecord() {
        return super.getRecord();
    }

    @Override
    public Reference asReference() {
        return new Reference(record.getWorldId(), record.getId());
    }

    @Override
    public WorldSnapshot getWorld() {
        return context.worlds().find(EntityKey.of(WORLDS.ID, record.getWorldId()))
                .map(record -> new WorldImpl(record, context))
                .orElseThrow(() -> new UnexpectedException("This region has no world", Severity.SYSTEM));
    }

    @Override
    public String getDescription() {
        return record.getDescription();
    }

    @Override
    public String getName() {
        return record.getName();
    }

    @Override
    public LorebookSnapshot lorebook() {
        return new LorebookImpl(context.lorebooks().getLorebookOf(this.record), context);
    }

    @Override
    public List<LocationSnapshot> getChildrenLocations() {
        return context.locations().getMatching(
                    EntityDataPayload.<LocationsRecord>builder()
                            .set(LOCATIONS.WORLD_ID, this.record.getWorldId())
                            .set(LOCATIONS.REGION_ID, this.record.getId())
                            .build()
                ).stream()
                .map(record -> (LocationSnapshot) new LocationImpl(record, context))
                .toList();
    }

    @Override
    public List<RegionSnapshot> getChildRegions() {
        return context.regions().getDepthOneChildrenOf(record).stream()
                .map(record -> (RegionSnapshot) new RegionImpl(record, context))
                .toList();
    }

    @Override
    public Optional<RegionSnapshot> parent() {
        if (this.record.getParentRegionId() == null) return Optional.empty();
        return Optional.of(
                context.regions().find(
                        EntityKey.<RegionRecord>builder()
                                .set(REGION.WORLD_ID, record.getWorldId())
                                .set(REGION.ID, record.getParentRegionId())
                                .build()
                )
                        .map(record -> new RegionImpl(record, context))
                        .orElseThrow(() -> new EntityNotFound("This region has a stale parent id", Severity.SYSTEM))
        );
    }
}
