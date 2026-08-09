package io.github.chechelpo.frplm.domain.world.location;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.domain.world.core.WorldTestContext;
import io.github.chechelpo.frplm.domain.world.region.RegionTestContext;
import io.github.chechelpo.frplm.interfaces.DBReload;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.test_utils.TestText;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATIONS;
import static org.junit.jupiter.api.Assertions.*;

@TestComponent
@Import({WorldTestContext.class, RegionTestContext.class})
public class LocationTestContext implements DBReload {
    public final WorldTestContext worldTestContext;
    public final LocationsService service;
    final LocationFieldsHelper fields;
    public final RegionTestContext regionTestContext;

    LocationTestContext(
            WorldTestContext worldTestContext,
            LocationFieldsHelper fields,
            LocationsService service,
            RegionTestContext regionTestContext) {
        this.worldTestContext = worldTestContext;
        this.fields = fields;
        this.service = service;
        this.regionTestContext = regionTestContext;
    }

    public List<LocationsRecord> createAndGetTestLocationsOfSameWorld(int locationAmount) {
        List<EntityDataPayload<LocationsRecord>> locationsData = new ArrayList<>(locationAmount);
        long seed = 10;
        RegionRecord regionRecord = regionTestContext.createRegions(1).getFirst();

        for (int i = 0; i < locationAmount; i++)
            locationsData.add(EntityDataPayload.<LocationsRecord>builder()
                    .set(LOCATIONS.WORLD_ID, regionRecord.getWorldId())
                    .set(LOCATIONS.REGION_ID, regionRecord.getId())
                    .set(LOCATIONS.NAME, TestText.randomText(seed + i, 0, 255))
                    .build()
            );

        return locationsData.stream().map(
                data -> assertDoesNotThrow(() -> service.createAndGet(data))
        ).toList();
    }

    @Override
    public void reload() {
        worldTestContext.reload();
    }
}
