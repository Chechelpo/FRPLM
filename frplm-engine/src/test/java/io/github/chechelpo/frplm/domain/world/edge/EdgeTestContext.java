package io.github.chechelpo.frplm.domain.world.edge;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.domain.world.location.LocationTestContext;
import io.github.chechelpo.frplm.interfaces.DBReload;
import chechelpo.frplm.jooq.generated.tables.records.LocationEdgesRecord;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.LOCATION_EDGES;

@TestComponent
public class EdgeTestContext implements DBReload {
    @Autowired
    public LocationTestContext locations;
    @Autowired
    public EdgeService service;
    @Autowired
    EdgeFieldsHelper fields;


    public void link(int worldID, int locationId, int otherLocationID){
        service.createAndGet(EntityDataPayload.<LocationEdgesRecord>builder()
                        .set(LOCATION_EDGES.WORLD_ID, worldID)
                        .set(LOCATION_EDGES.FROM_LOCATION_ID, locationId)
                        .set(LOCATION_EDGES.TO_LOCATION_ID, otherLocationID)
                        .build()
        );
    }
    /** Links locations i -> i + 1*/
    public void linkLinear(List<LocationsRecord> locationsToLink){
        if (locationsToLink == null || locationsToLink.isEmpty()) throw new IllegalArgumentException("Locations list is empty or null");
        int worldID = locationsToLink.getFirst().getWorldId();
        for (int i = 0; i < locationsToLink.size() - 1; i++)
            link(worldID, locationsToLink.get(i).getId(), locationsToLink.get(i+1).getId());
    }
    @Override
    public void reload() {
        locations.reload();
    }
}
