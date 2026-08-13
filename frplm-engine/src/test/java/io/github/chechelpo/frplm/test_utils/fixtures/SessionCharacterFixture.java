package io.github.chechelpo.frplm.test_utils.fixtures;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.domain.sessions.core.SessionService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import org.jetbrains.annotations.Contract;
import org.jooq.TableField;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class SessionCharacterFixture extends EntityFixtures<SessionsRecord, SessionService> {
    SessionCharacterFixture(SessionService service, EntityFixtureFactory fixtures, @NonNull String seed) {
        super(service, fixtures, seed);
    }

    @Override
    protected Set<TableField<SessionsRecord, ?>> doNotGenerateFields() {
        return Set.of();
    }

    @Contract(pure = true)
    @Override
    protected @NonNull List<Consumer<EntityDataPayload<SessionsRecord>>> getFunctionsToAssignForeignFields(EntityDataPayload<SessionsRecord> sample) {
        return List.of();
    }
}
