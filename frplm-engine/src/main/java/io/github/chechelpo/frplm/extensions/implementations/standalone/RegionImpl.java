package io.github.chechelpo.frplm.extensions.implementations.standalone;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.exceptions.runtime.UnexpectedException;
import chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.extensions.api.standalone.LocationSnapshot;
import io.github.chechelpo.frplm.extensions.api.standalone.LorebookSnapshot;
import io.github.chechelpo.frplm.extensions.api.standalone.RegionSnapshot;
import io.github.chechelpo.frplm.extensions.api.standalone.WorldSnapshot;

import java.util.List;
import java.util.Optional;

import static chechelpo.frplm.jooq.generated.Tables.REGION;
import static chechelpo.frplm.jooq.generated.Tables.WORLDS;

public class RegionImpl extends StandaloneEntity<RegionRecord> implements RegionSnapshot {
    protected RegionImpl(RegionRecord record, ExtensionContext context) {
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
    public LorebookSnapshot lorebook() {
        return new LorebookImpl(context.lorebooks().getLorebookOf(this.record), context);
    }

    @Override
    public List<LocationSnapshot> getChildrenLocations() {
        return context.locations().getLocationsOfRegion(this.record.getWorldId(), this.record.getId()).stream()
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
