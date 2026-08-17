package io.github.chechelpo.frplm.test_utils.fixtures;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.domain.sessions.session_characters.SessionCharacterService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionCharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import org.jetbrains.annotations.Contract;
import org.jooq.TableField;
import org.jspecify.annotations.NonNull;

import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

public final class SessionCharacterFixture extends EntityFixtures<SessionCharactersRecord, SessionCharacterService> {
    private final SessionFixtures sessionFixtures;
    private final CharacterFixtures characterFixtures;

    SessionCharacterFixture(SessionCharacterService service, EntityFixtureFactory fixtures, @NonNull String seed) {
        super(service, fixtures, seed);
        this.sessionFixtures = fixtures.sessions(seed);
        this.characterFixtures = fixtures.characters(seed);
    }

    @Override
    protected Set<TableField<SessionCharactersRecord, ?>> doNotGenerateFields() {
        return Set.of(SESSION_CHARACTERS.LAST_MOVED_TICK_NUM);
    }

    @Contract(pure = true)
    @Override
    protected @NonNull DoActions<SessionCharactersRecord> getFunctionsToAssignForeignFields(EntityDataPayload<SessionCharactersRecord> sample) {
        DoActions<SessionCharactersRecord> actions = DoActions.instantiate(2);

        sample.getAssignment(SESSION_CHARACTERS.SESSION_ID)
                .ifUnassignedRun(() -> {
                    EntityDataPayload<SessionsRecord> sessionPayload = EntityDataPayload.empty();
                    if (sample.assigns(SESSION_CHARACTERS.PERMANENT_CHARACTER_ID))
                        sessionPayload.set(SESSIONS.WORLD_ID, sample.requireNonNull(SESSION_CHARACTERS.WORLD_ID));
                    sessionPayload.ifAssignedSet(SESSIONS.WORLD_ID, SESSION_CHARACTERS.WORLD_ID, sample);

                    SessionsRecord session = sessionFixtures.createOne(sessionPayload);

                    actions.add(
                            payload ->
                                    payload
                                            .ifUnassignedSet(SESSION_CHARACTERS.WORLD_ID, session.getWorldId())
                                            .ifUnassignedSet(SESSION_CHARACTERS.SESSION_ID, session.getId())
                    );
                    sample.ifUnassignedSet(SESSION_CHARACTERS.WORLD_ID, session.getWorldId());
                });

        /*sample.getAssignment(SESSION_CHARACTERS.PERMANENT_CHARACTER_ID)
                .ifUnassignedRun(
                        () -> {
                            CharactersRecord character = characterFixtures.createOne(
                                    CHARACTERS.WORLD_ID, sample.requireNonNull(SESSION_CHARACTERS.WORLD_ID)
                            );
                            actions.add(
                                    payload ->
                                            payload
                                                    .ifUnassignedSet(SESSION_CHARACTERS.WORLD_ID, character.getWorldId())
                                                    .ifUnassignedSet(SESSION_CHARACTERS.CURRENT_LOCATION_ID, character.getStartingLocationId())
                            );
                        }
                );*/

        return actions;
    }

    public void move(SessionCharactersRecord character, LocationsRecord toLocation) {
        if (character.getWorldId() != toLocation.getWorldId())
            throw new IllegalStateException("Mismatch in world");
        service().update(
                SESSION_CHARACTERS.CURRENT_LOCATION_ID, toLocation.getId(),
                character
        ).orElseThrow();
    }
}
