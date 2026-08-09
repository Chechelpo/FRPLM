package io.github.chechelpo.frplm.extensions.snapshot_mappers;

import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.extensions.api.standalone.RegionSnapshot;
import io.github.chechelpo.frplm.extensions.implementations.standalone.RegionImpl;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;

import static io.github.chechelpo.frplm.jooq.generated.Tables.REGION;

final class RegionMapper extends ReferenceMapper<RegionRecord, RegionSnapshot.Reference, RegionSnapshot> {
    RegionMapper(EntityReader<RegionRecord> reader) {
        super(
                RegionSnapshot.class,
                RegionSnapshot.Reference::fromString,
                RegionImpl::new,
                reference -> EntityKey.<RegionRecord>builder()
                        .set(REGION.WORLD_ID, reference.worldId())
                        .set(REGION.ID, reference.regionId())
                        .build(),
                reader
        );
    }

    @Override
    RegionSnapshot.Reference getExampleReference() {
        return new RegionSnapshot.Reference(1, 1);
    }
}
