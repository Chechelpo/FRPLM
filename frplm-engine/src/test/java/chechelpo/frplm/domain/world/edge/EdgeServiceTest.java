package chechelpo.frplm.domain.world.edge;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.domain.world.location.LocationTestContext;
import chechelpo.frplm.exceptions.runtime.Duplicate;
import chechelpo.frplm.jooq.generated.tables.records.LocationNeighborsRecord;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Objects;

import static chechelpo.frplm.jooq.generated.Tables.LOCATION_NEIGHBORS;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Sql(
        scripts = "classpath:db/schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Import({LocationTestContext.class, EdgeTestContext.class})
class EdgeServiceTest {
    @Autowired
    LocationTestContext locations;
    @Autowired
    EdgeTestContext edges;
    @Autowired
    EdgeService edgeService;
    @Autowired
    EdgeFieldsHelper fieldsHelper;

    @BeforeEach
    void setUp() {
        locations.reload();
    }

    @Test
    void linkingLocationToSameLocationDoesNothing() {
        int testAmount = 100;
        List<LocationsRecord> world1 = locations.createAndGetTestLocationsOfSameWorld(testAmount);

        for (int i = 0; i < testAmount; i++) {
            int finalI = i;
            assertThrows(
                    IllegalArgumentException.class, () ->
                            edgeService.createAndGet(EntityDataPayload.<LocationNeighborsRecord>builder()
                                    .set(LOCATION_NEIGHBORS.WORLD_ID, world1.get(finalI).getWorldId())
                                    .set(LOCATION_NEIGHBORS.LOCATION1_ID, world1.get(finalI).getId())
                                    .set(LOCATION_NEIGHBORS.LOCATION2_ID, world1.get(finalI).getId())
                                    .build()
                            ));
        }
    }


    @Test
    void rejectsLinkingLocationsOfDifferentWorlds() {
        int testAmount = 100;
        List<LocationsRecord> world1 = locations.createAndGetTestLocationsOfSameWorld(testAmount);
        List<LocationsRecord> world2 = locations.createAndGetTestLocationsOfSameWorld(testAmount);

        for (int i = 0; i < testAmount; i++) {
            int finalI = i;
            assertThrows(Exception.class, () -> {
                edgeService.createAndGet(EntityDataPayload.<LocationNeighborsRecord>builder()
                        .set(LOCATION_NEIGHBORS.WORLD_ID, world1.get(finalI).getWorldId())
                        .set(LOCATION_NEIGHBORS.LOCATION1_ID, world1.get(finalI).getId())
                        .set(LOCATION_NEIGHBORS.LOCATION2_ID, world2.get(finalI).getId())
                        .build()
                );
            });
            assertThrows(Exception.class, () -> {
                edgeService.createAndGet(EntityDataPayload.<LocationNeighborsRecord>builder()
                        .set(LOCATION_NEIGHBORS.WORLD_ID, world1.get(finalI).getWorldId())
                        .set(LOCATION_NEIGHBORS.LOCATION1_ID, world2.get(finalI).getId())
                        .set(LOCATION_NEIGHBORS.LOCATION2_ID, world1.get(finalI).getId())
                        .build()
                );
            });
        }
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

    @Test
    void createEdges_throwsOnIdempotentOrders(){
        int testAmount = 4;
        List<LocationsRecord> createdRecords = locations.createAndGetTestLocationsOfSameWorld(testAmount);

        LocationsRecord parent = createdRecords.getFirst();
        LocationsRecord first = createdRecords.get(1);
        LocationsRecord second = createdRecords.get(2);
        int worldId = parent.getWorldId();
        assert worldId == first.getWorldId() && Objects.equals(first.getWorldId(), second.getWorldId());

        assertDoesNotThrow(() -> edges.link(worldId, parent.getId(), first.getId()));
        assertDoesNotThrow(() -> edges.link(worldId, second.getId(), parent.getId()));

        assertThrows(
                Duplicate.class,
                () -> edges.link(worldId, first.getId(), parent.getId()),
                "Could link duplicate"
        );
        assertThrows(
                Duplicate.class,
                () -> edges.link(worldId, parent.getId(), second.getId()),
                "Could link duplicate"
        );

        List<LocationsRecord> actualNeighbours = edges.service.getNeighboursOf(parent);
        assertEquals(2, actualNeighbours.size(), "Found more neighbours than expected");
    }

}