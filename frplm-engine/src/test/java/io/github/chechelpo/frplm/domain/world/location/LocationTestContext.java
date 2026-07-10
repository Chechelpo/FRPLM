package io.github.chechelpo.frplm.domain.world.location;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.domain.world.core.WorldTestContext;
import io.github.chechelpo.frplm.interfaces.DBReload;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import io.github.chechelpo.frplm.test_utils.TestText;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATIONS;
import static org.junit.jupiter.api.Assertions.*;

@TestComponent
@Import(WorldTestContext.class)
public class LocationTestContext implements DBReload {
    public final WorldTestContext worldTestContext;
    public final LocationsService service;
    final LocationFieldsHelper fields;

    LocationTestContext(
            WorldTestContext worldTestContext,
            LocationFieldsHelper fields,
            LocationsService service
    ) {
        this.worldTestContext = worldTestContext;
        this.fields = fields;
        this.service = service;
    }

    public List<LocationsRecord> createAndGetTestLocationsOfSameWorld(int locationAmount) {
        List<WorldsRecord> worldsRecords = worldTestContext.createWorlds(1).createdRecords();
        assertEquals(1, worldsRecords.size());
        List<EntityDataPayload<LocationsRecord>> locationsData = new ArrayList<>(locationAmount);
        long seed = 10;
        WorldsRecord world = worldsRecords.getFirst();
        for (int i = 0; i < locationAmount; i++)
            locationsData.add(EntityDataPayload.<LocationsRecord>builder()
                    .set(LOCATIONS.WORLD_ID, world.getId())
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
