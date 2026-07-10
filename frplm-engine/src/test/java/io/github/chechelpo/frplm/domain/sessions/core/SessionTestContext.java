package io.github.chechelpo.frplm.domain.sessions.core;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.domain.character.core.CharacterCoreTestContext;
import io.github.chechelpo.frplm.domain.character.starting_locations.StartingLocationTestContext;
import io.github.chechelpo.frplm.domain.world.location.LocationTestContext;
import io.github.chechelpo.frplm.interfaces.DBReload;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

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

    /**
     * @param userCharacter
     * @param session
     * @param sessionLocations (unlinked)
     */
    public record SessionContext(CharactersRecord userCharacter, SessionsRecord session, List<LocationsRecord> sessionLocations) {}
    /** Locations of this session are not linked. Character starts at the first location of the list */
    public SessionContext createSession(int locationsAmount, int charactersPerLocation){
        List<LocationsRecord> locationsOfSession = locations.createAndGetTestLocationsOfSameWorld(locationsAmount);
        //Create other locations of other worlds
        for (int i = 0; i < 20; i++) {
            locations.createAndGetTestLocationsOfSameWorld(10);
        }
        List<CharactersRecord> charactersRecords = characters.createAndGetRecords(locationsAmount * charactersPerLocation);

        CharactersRecord userCharacter = charactersRecords.getFirst();
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

        return new SessionContext(userCharacter, newSession, locationsOfSession);
    }

}
