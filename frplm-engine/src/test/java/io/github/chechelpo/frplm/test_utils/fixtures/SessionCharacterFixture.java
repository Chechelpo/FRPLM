package io.github.chechelpo.frplm.test_utils.fixtures;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.domain.sessions.core.SessionService;
import io.github.chechelpo.frplm.domain.sessions.session_characters.SessionCharacterService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionCharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import org.jetbrains.annotations.Contract;
import org.jooq.TableField;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static io.github.chechelpo.frplm.jooq.generated.Tables.SESSIONS;
import static io.github.chechelpo.frplm.jooq.generated.Tables.SESSION_CHARACTERS;

public final class SessionCharacterFixture extends EntityFixtures<SessionCharactersRecord, SessionCharacterService> {
    private final SessionFixtures sessionFixtures;
    SessionCharacterFixture(SessionCharacterService service, EntityFixtureFactory fixtures, @NonNull String seed) {
        super(service, fixtures, seed);
        this.sessionFixtures = fixtures.sessions(seed);
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

                    SessionsRecord session = sessionFixtures.createOne(sessionPayload);
                    actions.add(
                            payload ->
                                    payload
                                            .set(SESSION_CHARACTERS.WORLD_ID, session.getWorldId())
                                            .set(SESSION_CHARACTERS.SESSION_ID, session.getId())
                    );
                });

        return actions;
    }
}
