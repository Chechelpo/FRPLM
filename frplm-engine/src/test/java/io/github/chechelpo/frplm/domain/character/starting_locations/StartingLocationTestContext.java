package io.github.chechelpo.frplm.domain.character.starting_locations;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.domain.world.location.LocationTestContext;
import io.github.chechelpo.frplm.interfaces.DBReload;
import io.github.chechelpo.frplm.jooq.generated.tables.records.StartingLocationsRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import static io.github.chechelpo.frplm.jooq.generated.Tables.STARTING_LOCATIONS;

@TestComponent
public class StartingLocationTestContext implements DBReload {
    @Autowired
    public LocationTestContext locations;
    @Autowired
    public StartingLocationsService service;
    @Autowired
    StartingLocationFieldsHelper fields;

    public void setStartingAt(int worldID, int locationID, int characterID) {
        service.createAndGet(EntityDataPayload.<StartingLocationsRecord>builder()
                        .set(STARTING_LOCATIONS.WORLD_ID, worldID)
                        .set(STARTING_LOCATIONS.LOCATION_ID, locationID)
                        .set(STARTING_LOCATIONS.CHARACTER_ID, characterID)
                        .build()
        );
    }

    @Override
    public void reload() {
        locations.reload();
    }
}
