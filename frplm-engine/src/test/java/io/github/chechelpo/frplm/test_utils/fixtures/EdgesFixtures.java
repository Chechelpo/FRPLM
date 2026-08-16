package io.github.chechelpo.frplm.test_utils.fixtures;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.domain.world.edge.EdgeService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationEdgesRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import org.jooq.TableField;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATION_EDGES;

public class EdgesFixtures extends EntityFixtures<LocationEdgesRecord, EdgeService> {
    protected EdgesFixtures(EdgeService service, EntityFixtureFactory fixtureFactory, @NonNull String seed) {
        super(service, fixtureFactory, seed);
    }

    @Override
    protected DoActions<LocationEdgesRecord> getFunctionsToAssignForeignFields(EntityDataPayload<LocationEdgesRecord> sample) {
        return null;
    }

    @Override
    protected Set<TableField<LocationEdgesRecord, ?>> doNotGenerateFields() {
        return Set.of();
    }


    public void linkLinear(@NonNull List<LocationsRecord> locations){
        if (locations.size() < 2) return;
        int expectedWorldId = locations.getFirst().getWorldId();

        for (int i = 0 ; i < locations.size() - 1 ; i++) {
            LocationsRecord from = locations.get(i);
            LocationsRecord to = locations.get(i + 1);
            if (from.getWorldId() != expectedWorldId || to.getWorldId() != expectedWorldId)
                throw new IllegalArgumentException("The list of locations are not from the same world");
            if (Objects.equals(to.getId(), from.getId()))
                throw new IllegalArgumentException("Location " + from.getName() + " is duplicated");
            service().createAndGet(
                    EntityDataPayload.<LocationEdgesRecord>builder()
                            .set(LOCATION_EDGES.WORLD_ID, expectedWorldId)
                            .set(LOCATION_EDGES.FROM_LOCATION_ID, from.getId())
                            .set(LOCATION_EDGES.TO_LOCATION_ID, to.getId())
                            .build()
            );
        }
    }
}
