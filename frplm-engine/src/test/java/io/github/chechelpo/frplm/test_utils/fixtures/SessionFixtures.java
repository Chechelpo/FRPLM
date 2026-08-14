package io.github.chechelpo.frplm.test_utils.fixtures;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.domain.sessions.core.SessionService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import org.jetbrains.annotations.Unmodifiable;
import org.jooq.TableField;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static io.github.chechelpo.frplm.jooq.generated.Tables.CHARACTERS;
import static io.github.chechelpo.frplm.jooq.generated.Tables.SESSIONS;

public final class SessionFixtures extends EntityFixtures<SessionsRecord, SessionService> {
    private final CharacterFixtures characterFixtures;
    SessionFixtures(SessionService service, EntityFixtureFactory fixtures, @NonNull String seed) {
        super(service, fixtures, seed);
        this.characterFixtures = fixtures.characters(seed + "-characters");
    }

    @Override
    protected @NonNull @Unmodifiable Set<TableField<SessionsRecord, ?>> doNotGenerateFields() {
        return Set.of(SESSIONS.USER_PERSONA_ID, SESSIONS.NEXT_CHARACTER_ID, SESSIONS.NEXT_LOCATION_ID);
    }

    @Override
    protected @NonNull DoActions<SessionsRecord> getFunctionsToAssignForeignFields(
            EntityDataPayload<SessionsRecord> sample
    ) {
        DoActions<SessionsRecord> consumers = DoActions.instantiate(1);
        sample.getAssignment(SESSIONS.USER_PERSONA_ID)
                .ifUnassignedRun(
                        () -> {
                            CharactersRecord character = characterFixtures.addAndCreateTo(
                                    EntityDataPayload.<CharactersRecord>builder()
                                            .copyIfAssigned(CHARACTERS.WORLD_ID, SESSIONS.WORLD_ID, sample)
                                            .build()
                            );
                            consumers.add(
                                    payload -> payload
                                            .set(SESSIONS.WORLD_ID, character.getWorldId())
                                            .set(SESSIONS.USER_PERSONA_ID, character.getId())
                            );
                        }
                );

        return consumers;
    }
}
