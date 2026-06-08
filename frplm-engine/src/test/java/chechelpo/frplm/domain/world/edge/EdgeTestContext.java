package chechelpo.frplm.domain.world.edge;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.domain.world.location.LocationTestContext;
import chechelpo.frplm.interfaces.DBReload;
import chechelpo.frplm.jooq.generated.tables.records.LocationNeighborsRecord;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.LOCATION_NEIGHBORS;

@TestComponent
public class EdgeTestContext implements DBReload {
    @Autowired
    public LocationTestContext locations;
    @Autowired
    public EdgeService service;
    @Autowired
    EdgeFieldsHelper fields;


    public void link(int worldID, int locationId, int otherLocationID){
        service.createAndGet(EntityDataPayload.<LocationNeighborsRecord>builder()
                        .set(LOCATION_NEIGHBORS.WORLD_ID, worldID)
                        .set(LOCATION_NEIGHBORS.LOCATION1_ID, locationId)
                        .set(LOCATION_NEIGHBORS.LOCATION2_ID, otherLocationID)
                        .build()
        );
    }
    /** Links locations i -> i + 1*/
    public void linkLinear(List<LocationsRecord> locationsToLink){
        if (locationsToLink == null || locationsToLink.isEmpty()) throw new IllegalArgumentException("Locations list is empty or null");
        int worldID = locationsToLink.getFirst().getWorldId();
        for (int i = 0; i < locationsToLink.size() - 1; i++) {
            link(worldID, locationsToLink.get(i).getId(), locationsToLink.get(i+1).getId());
            assert service.isNeighbour(
                    locations.service.keyOf(locationsToLink.get(i)),
                    locations.service.keyOf(locationsToLink.get(i+1))
            ) : "Error when linking locations";
        }
    }
    @Override
    public void reload() {
        locations.reload();
    }
}
