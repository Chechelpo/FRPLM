package chechelpo.frplm.domain.character.starting_locations;

import chechelpo.frplm.domain.world.location.LocationTestContext;
import chechelpo.frplm.interfaces.DBReload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class StartingLocationTestContext implements DBReload {
    @Autowired
    public LocationTestContext locations;
    @Autowired
    public StartingLocationsService service;
    @Autowired
    StartingLocationFieldsHelper fields;

    @Override
    public void reload() {
        locations.reload();
    }
}
