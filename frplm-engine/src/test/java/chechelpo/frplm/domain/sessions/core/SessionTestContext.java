package chechelpo.frplm.domain.sessions.core;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.domain.character.core.CharacterCoreTestContext;
import chechelpo.frplm.domain.character.starting_locations.StartingLocationTestContext;
import chechelpo.frplm.domain.world.location.LocationTestContext;
import chechelpo.frplm.interfaces.DBReload;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import chechelpo.frplm.jooq.generated.tables.records.StartingLocationsRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.*;
import static chechelpo.frplm.jooq.generated.Tables.SESSIONS;

@TestComponent
@Import({CharacterCoreTestContext.class, StartingLocationTestContext.class, LocationTestContext.class})
public class SessionTestContext implements DBReload {
    @Autowired
    public SessionService service;
    @Autowired
    public CharacterCoreTestContext characters;
    @Autowired
    public StartingLocationTestContext startingLocations;
    @Autowired
    public LocationTestContext locations;
    @Autowired
    SessionFieldsHelper fields;

    @Override
    public void reload() {
        characters.reload();
        startingLocations.reload();
        locations.reload();
    }

    public record SessionContext(CharactersRecord userCharacter, SessionsRecord session){}
    /** Locations of this session are not linked */
    public SessionContext createSession(int locationsAmount, int charactersPerLocation){
        List<LocationsRecord> locationsOfSession = locations.createAndGetTestLocationsOfSameWorld(locationsAmount);
        //Create other locations of other worlds
        for (int i = 0; i < 20; i++) {
            locations.createAndGetTestLocationsOfSameWorld(10);
        }
        List<CharactersRecord> charactersRecords = characters.createAndGetRecords(locationsAmount * charactersPerLocation);

        CharactersRecord userCharacter = charactersRecords.getLast();
        characters.service.update(
                characters.service.keyOf(userCharacter),
                EntityDataPayload.of(CHARACTERS.CAN_BE_USER, true)
        );

        HashMap<Integer, List<CharactersRecord>> locationIDToCharactersStarting = new HashMap<>(locationsAmount);
        int characterIndex = 0;
        for (LocationsRecord locationsRecord : locationsOfSession) {
            List<CharactersRecord> charactersStartingHere = new ArrayList<>(charactersPerLocation);
            for (int i = characterIndex; i < characterIndex + charactersPerLocation; i++) {
                startingLocations.setStartingAt(locationsRecord.getWorldId(), locationsRecord.getId(), charactersRecords.get(i).getId());
                charactersStartingHere.add(charactersRecords.get(i));
            }
            characterIndex += charactersPerLocation;
            locationIDToCharactersStarting.put(locationsRecord.getId(), charactersStartingHere);
        }

        SessionsRecord newSession = service.createAndGet(EntityDataPayload.<SessionsRecord>builder()
                .set(SESSIONS.NAME, "SessionTest")
                .set(SESSIONS.USER_PERSONA_ID, userCharacter.getId())
                .set(SESSIONS.WORLD_ID, locationsOfSession.getFirst().getWorldId())
                .build()
        );

        return new SessionContext(userCharacter, newSession);
    }

}
