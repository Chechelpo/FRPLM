package chechelpo.frplm.domain.sessions.movement;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.domain.character.core.CharacterCoreTestContext;
import chechelpo.frplm.domain.character.starting_locations.StartingLocationTestContext;
import chechelpo.frplm.domain.sessions.core.SessionTestContext;
import chechelpo.frplm.domain.sessions.messages.MessageTestContext;
import chechelpo.frplm.domain.world.edge.EdgeTestContext;
import chechelpo.frplm.domain.world.location.LocationTestContext;
import chechelpo.frplm.interfaces.DBReload;
import chechelpo.frplm.jooq.generated.tables.records.CurrentLocationsRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;

import static chechelpo.frplm.jooq.generated.Tables.CURRENT_LOCATIONS;

@TestComponent
@Import({MessageTestContext.class, CharacterCoreTestContext.class, StartingLocationTestContext.class, SessionTestContext.class, LocationTestContext.class, EdgeTestContext.class})
public class CurrentLocationTestContext implements DBReload {
    @Autowired LocationsEventReactor locationsEventReactor;
    @Autowired
    public MessageTestContext messages;
    @Autowired
    private LocationTestContext locationTestContext;
    @Autowired
    private CharacterCoreTestContext characterCoreTestContext;
    @Autowired
    private StartingLocationTestContext startingLocationTestContext;
    @Autowired
    private SessionTestContext sessionTestContext;
    @Autowired
    public CurrentLocationService service;
    @Autowired
    CurrentLocationFields fields;
    @Autowired
    private EdgeTestContext edgeTestContext;

    public boolean move(int characterID, int worldId, int atTick, int toLocationId, int sessionId){
        return service.update(
                EntityKey.<CurrentLocationsRecord>builder()
                        .set(CURRENT_LOCATIONS.SESSION_ID, sessionId)
                        .set(CURRENT_LOCATIONS.CHARACTER_ID, characterID)
                        .build(),
                EntityDataPayload.<CurrentLocationsRecord>builder()
                        .set(CURRENT_LOCATIONS.WORLD_ID, worldId)
                        .set(CURRENT_LOCATIONS.LOCATION_ID, toLocationId)
                        .set(CURRENT_LOCATIONS.TICK_NUM, atTick)
                        .build()
        );
    }

    @Override
    public void reload(){
        messages.reload();
        locationTestContext.reload();
        characterCoreTestContext.reload();
        startingLocationTestContext.reload();
        sessionTestContext.reload();
    }
}
