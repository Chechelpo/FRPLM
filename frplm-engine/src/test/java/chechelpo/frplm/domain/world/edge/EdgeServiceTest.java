package chechelpo.frplm.domain.world.edge;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.domain.world.location.LocationTestContext;
import chechelpo.frplm.jooq.generated.tables.records.LocationNeighborsRecord;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.LOCATION_NEIGHBORS;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Sql(
        scripts = "classpath:db/schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Import(LocationTestContext.class)
class EdgeServiceTest {
    @Autowired LocationTestContext locations;
    @Autowired EdgeService edgeService;
    @Autowired EdgeFieldsHelper fieldsHelper;

    @BeforeEach
    void setUp() {
        locations.reload();
    }

    @Test
    void testNeighbours() {
        int testAmount = 100;
        List<LocationsRecord> createdRecords = locations.createAndGetTestLocationsOfSameWorld(testAmount);
        assertEquals(testAmount, createdRecords.size());

        for (int i = 0; i < testAmount - 1; i++) {
            LocationsRecord fromLocation = createdRecords.get(i);
            LocationsRecord toLocation = createdRecords.get(i + 1);
            assertEquals(fromLocation.getWorldId(), toLocation.getWorldId(), "These are from two different worlds");
            assertNotEquals(fromLocation, toLocation);

            edgeService.createAndGet(EntityDataPayload.<LocationNeighborsRecord>builder()
                    .set(LOCATION_NEIGHBORS.WORLD_ID, fromLocation.getWorldId())
                    .set(LOCATION_NEIGHBORS.LOCATION1_ID, fromLocation.getId())
                    .set(LOCATION_NEIGHBORS.LOCATION2_ID, toLocation.getId())
                    .build()
            );

            EntityKey<LocationsRecord> fromKey = locations.service.keyOf(fromLocation);
            EntityKey<LocationsRecord> toKey = locations.service.keyOf(toLocation);
            assertNotEquals(fromKey, toKey, "Keys are the same");

            assertTrue(edgeService.isNeighbour(fromKey, toKey), "Locations are not connected");
            assertTrue(edgeService.isNeighbour(toKey, fromKey), "Neighbours is treated as a directed graph");

            List<LocationsRecord> fromNeighbours = edgeService.getNeighboursOf(fromLocation);
            assertEquals(1, fromNeighbours.size(), "Neighbours size should be the same");
            assertTrue(fromNeighbours.contains(toLocation), "Neighbours are not connected");

            List<LocationsRecord> toNeighbours = edgeService.getNeighboursOf(toLocation);
            assertEquals(1, toNeighbours.size(), "Neighbours size should be the same");
            assertTrue(toNeighbours.contains(fromLocation), "Neighbours are not connected");

            if (i % 2 == 0) assertTrue(
                    edgeService.delete(EntityKey.<LocationNeighborsRecord>builder()
                                    .set(LOCATION_NEIGHBORS.WORLD_ID, fromLocation.getWorldId())
                                    .set(LOCATION_NEIGHBORS.LOCATION1_ID, fromLocation.getId())
                                    .set(LOCATION_NEIGHBORS.LOCATION2_ID, toLocation.getId())
                                    .build()
                    ),
                    "Standard: Couldn't delink location neighbours"
            );
            else assertTrue(
                    edgeService.delete(EntityKey.<LocationNeighborsRecord>builder()
                            .set(LOCATION_NEIGHBORS.WORLD_ID, fromLocation.getWorldId())
                            .set(LOCATION_NEIGHBORS.LOCATION2_ID, fromLocation.getId())
                            .set(LOCATION_NEIGHBORS.LOCATION1_ID, toLocation.getId())
                            .build()
                    ),
                    "Inverse: Couldn't delink location neighbours"
            );

            assertFalse(edgeService.isNeighbour(fromKey, toKey), "Neighbours are still connected");
        }
    }
}