package io.github.chechelpo.frplm.domain.world.edge;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.domain.world.location.LocationTestContext;
import io.github.chechelpo.frplm.exceptions.runtime.InvalidValue;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationEdgesRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Objects;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATIONS;
import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATION_EDGES;
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
    void linkingToSameLocationThrows() {
        int testAmount = 100;
        List<LocationsRecord> world1 = locations.createAndGetTestLocationsOfSameWorld(testAmount);

        for (int i = 0; i < testAmount; i++) {
            int finalI = i;
            assertThrows(
                    InvalidValue.class,
                    () -> edgeService.createAndGet(EntityDataPayload.<LocationEdgesRecord>builder()
                                    .set(LOCATION_EDGES.WORLD_ID, world1.get(finalI).getWorldId())
                                    .set(LOCATION_EDGES.FROM_LOCATION_ID, world1.get(finalI).getId())
                                    .set(LOCATION_EDGES.TO_LOCATION_ID, world1.get(finalI).getId())
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
                edgeService.createAndGet(EntityDataPayload.<LocationEdgesRecord>builder()
                        .set(LOCATION_EDGES.WORLD_ID, world1.get(finalI).getWorldId())
                        .set(LOCATION_EDGES.FROM_LOCATION_ID, world1.get(finalI).getId())
                        .set(LOCATION_EDGES.TO_LOCATION_ID, world2.get(finalI).getId())
                        .build()
                );
            });
            assertThrows(Exception.class, () -> {
                edgeService.createAndGet(EntityDataPayload.<LocationEdgesRecord>builder()
                        .set(LOCATION_EDGES.WORLD_ID, world1.get(finalI).getWorldId())
                        .set(LOCATION_EDGES.FROM_LOCATION_ID, world2.get(finalI).getId())
                        .set(LOCATION_EDGES.TO_LOCATION_ID, world1.get(finalI).getId())
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

            edgeService.createAndGet(EntityDataPayload.<LocationEdgesRecord>builder()
                    .set(LOCATION_EDGES.WORLD_ID, fromLocation.getWorldId())
                    .set(LOCATION_EDGES.FROM_LOCATION_ID, fromLocation.getId())
                    .set(LOCATION_EDGES.TO_LOCATION_ID, toLocation.getId())
                    .build()
            );

            EntityKey<LocationsRecord> fromKey = locations.service.keyOf(fromLocation);
            EntityKey<LocationsRecord> toKey = locations.service.keyOf(toLocation);
            int worldId = fromKey.requireValue(LOCATIONS.WORLD_ID);
            assertNotEquals(fromKey, toKey, "Keys are the same");


            List<LocationsRecord> fromNeighbours = edgeService.neighboursOf(fromKey);
            assertEquals(1, fromNeighbours.size(), "Neighbours size should be the same");
            assertTrue(fromNeighbours.contains(toLocation), "Neighbours are not connected");

            List<LocationsRecord> toNeighbours = edgeService.neighboursOf(toLocation);
            assertEquals(1, toNeighbours.size(), "Neighbours size should be the same");
            assertTrue(toNeighbours.contains(fromLocation), "Neighbours are not connected");

            assertTrue(
                    edgeService.delete(EntityKey.<LocationEdgesRecord>builder()
                            .set(LOCATION_EDGES.WORLD_ID, fromLocation.getWorldId())
                            .set(LOCATION_EDGES.FROM_LOCATION_ID, fromLocation.getId())
                            .set(LOCATION_EDGES.TO_LOCATION_ID, toLocation.getId())
                            .build()
                    ),
                    "Standard: Couldn't delink location neighbours"
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

        assertDoesNotThrow(
                () -> edges.link(worldId, first.getId(), parent.getId()),
                "Could link duplicate"
        );
        assertDoesNotThrow(
                () -> edges.link(worldId, parent.getId(), second.getId()),
                "Could link duplicate"
        );

        List<LocationsRecord> actualNeighbours = edges.service.neighboursOf(parent);
        assertEquals(2, actualNeighbours.size(), "Found more neighbours than expected");
    }

}