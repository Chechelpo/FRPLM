package io.github.chechelpo.frplm.domain.sessions.movement;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.domain.character.core.CharacterCoreTestContext;
import io.github.chechelpo.frplm.domain.character.starting_locations.StartingLocationTestContext;
import io.github.chechelpo.frplm.domain.sessions.core.SessionTestContext;
import io.github.chechelpo.frplm.domain.sessions.messages.MessageTestContext;
import io.github.chechelpo.frplm.domain.world.edge.EdgeTestContext;
import io.github.chechelpo.frplm.domain.world.location.LocationTestContext;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.exceptions.runtime.UnsupportedAction;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationEdgesRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import static io.github.chechelpo.frplm.jooq.generated.Tables.CHARACTERS;
import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATION_EDGES;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Sql(
        scripts = "classpath:db/schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Import({MessageTestContext.class, LocationTestContext.class, SessionTestContext.class,
        StartingLocationTestContext.class, CharacterCoreTestContext.class, EdgeTestContext.class,
        CurrentLocationTestContext.class
})
public class FullMovementsTest {
    @Autowired
    MessageTestContext messages;
    @Autowired
    private LocationTestContext locationTestContext;
    @Autowired
    private CharacterCoreTestContext characterCoreTestContext;
    @Autowired
    private StartingLocationTestContext startingLocationTestContext;
    @Autowired
    private SessionTestContext sessionTestContext;
    @Autowired
    private EdgeTestContext edgeTestContext;
    @Autowired
    private CurrentLocationTestContext currentLocationTestContext;
    @Autowired
    private Movements movements;

    @BeforeEach
    void setUp() {
        messages.reload();
        locationTestContext.reload();
        characterCoreTestContext.reload();
        startingLocationTestContext.reload();
        sessionTestContext.reload();
        edgeTestContext.reload();
        currentLocationTestContext.reload();
    }


    @Test
    void move_throwsOnNotNeighbours() {
        MessageTestContext.Context context = messages.createSessionWithMessages(10, 3);
        assertThrows(
                EntityNotFound.class,
                () -> movements.move(
                        context.sessionContext().session().getId(),
                        context.sessionContext().session().getUserPersonaId(),
                        context.sessionContext().sessionLocations().get(2).getId())
        );
    }
    @Test
    void move_throwsOnUnknownCharacter() {
        MessageTestContext.Context context = messages.createSessionWithMessages(10, 3);
        CharactersRecord randomCharacter = messages.sessions.characters.service.createAndGet(
                EntityDataPayload.of(CHARACTERS.NAME, "test")
        );
        assertThrows(
                EntityNotFound.class,
                () -> movements.move(
                        context.sessionContext().session().getId(),
                        randomCharacter.getId(),
                        context.sessionContext().sessionLocations().get(2).getId())
        );
    }
    @Test
    void move_rejectsMovingOnNonTraversableEdge(){
        MessageTestContext.Context context = messages.createSessionWithMessages(10, 3);
        int characterId = context.sessionContext().userCharacter().getId();
        LocationsRecord fromLocation = currentLocationTestContext.service.getLocationOf(
                context.sessionContext().userCharacter(),
                context.sessionContext().session()
        );
        LocationsRecord toLocation = context.sessionContext().sessionLocations().get(1);

        edgeTestContext.service.createAndGet(
                EntityDataPayload.<LocationEdgesRecord>builder()
                        .set(LOCATION_EDGES.WORLD_ID, fromLocation.getWorldId())
                        .set(LOCATION_EDGES.FROM_LOCATION_ID, fromLocation.getId())
                        .set(LOCATION_EDGES.TO_LOCATION_ID, toLocation.getId())
                        .set(LOCATION_EDGES.TRAVERSABLE, false)
                        .build()
        );

        assertThrows(
                UnsupportedAction.class,
                () -> movements.move(
                        context.sessionContext().session().getId(),
                        characterId,
                        toLocation.getId()
                )
        );
    }
    @Test
    void move_throwsOnUnknownSessionId(){
        MessageTestContext.Context context = messages.createSessionWithMessages(10, 3);
        assertThrows(
                EntityNotFound.class,
                () -> movements.move(
                        -1,
                        context.sessionContext().session().getUserPersonaId(),
                        context.sessionContext().sessionLocations().get(2).getId())
        );
    }
    @Test
    void move_doesNothingIfInFirstMessage(){
        MessageTestContext.Context context = messages.createSessionWithMessages(10, 0);
        assertFalse(
                movements.move(
                        context.sessionContext().session().getId(),
                        context.sessionContext().session().getUserPersonaId(),
                        2
                )
        );
    }

}
