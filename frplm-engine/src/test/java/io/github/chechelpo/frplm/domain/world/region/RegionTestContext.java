package io.github.chechelpo.frplm.domain.world.region;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.domain.world.core.WorldTestContext;
import io.github.chechelpo.frplm.domain.world.location.LocationTestContext;
import io.github.chechelpo.frplm.interfaces.DBReload;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.junit.jupiter.params.shadow.de.siegmar.fastcsv.util.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;
import static org.junit.jupiter.api.Assertions.*;

@TestComponent
@Import({WorldTestContext.class, LocationTestContext.class})
public class RegionTestContext implements DBReload {
    @Autowired
    WorldTestContext worlds;
    @Autowired
    public RegionService service;
    @Autowired
    RegionFields fields;
    @Autowired
    LocationTestContext locationTestContext;

    public List<RegionRecord> createRegions(int number, @Nullable Integer locationsPerRegion) {
        WorldsRecord world = worlds.service.createAndGet(EntityDataPayload.of(WORLDS.NAME, "Region world"));

        List<RegionRecord> records = new ArrayList<>(number);
        int worldId = world.getId();
        final boolean injectLocations = locationsPerRegion != null;
        for (int i = 0; i < number; i++) {
            RegionRecord regionRecord = service.createAndGet(
                    EntityDataPayload.<RegionRecord>builder()
                            .set(REGION.WORLD_ID, worldId)
                            .set(REGION.NAME, "Region " + i)
                            .build()
            );
            int regionId = regionRecord.getId();
            if (injectLocations){
                for (int j = 0 ; j < locationsPerRegion ; j++){
                    locationTestContext.service.createAndGet(
                            EntityDataPayload.<LocationsRecord>builder()
                                    .set(LOCATIONS.WORLD_ID, worldId)
                                    .set(LOCATIONS.REGION_ID, regionId)
                                    .set(LOCATIONS.NAME, "location num %s of worldId : %s, regionId: %s".formatted(j, worldId, regionId))
                                    .build()
                    );
                }
            }
            records.add(regionRecord);
        }

        return records;
    }

    public void linkRegion(RegionRecord parent, RegionRecord child){
        assertEquals(parent.getWorldId(), child.getWorldId(), "Mismatch in world id");
        assertTrue(
                service.update(service.keyOf(child), EntityDataPayload.of(REGION.PARENT_REGION_ID, parent.getId())),
                "Could not link region"
        );
    }

    public void linkRegionsAsAcyclicChains(
            List<RegionRecord> regions
    ) {
        Map<Integer, List<RegionRecord>> regionsByWorld = regions.stream()
                .collect(Collectors.groupingBy(
                        RegionRecord::getWorldId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        for (List<RegionRecord> worldRegions : regionsByWorld.values()) {
            for (int i = 1; i < worldRegions.size(); i++) {
                RegionRecord parent = worldRegions.get(i - 1);
                RegionRecord child = worldRegions.get(i);

                linkRegion(parent, child);
            }
        }
    }
    @Override
    public void reload() {
        worlds.reload();
    }
}
