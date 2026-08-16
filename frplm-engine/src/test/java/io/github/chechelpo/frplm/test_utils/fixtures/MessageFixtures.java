package io.github.chechelpo.frplm.test_utils.fixtures;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.domain.sessions.messages.MessageService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import org.jooq.TableField;
import org.jspecify.annotations.NonNull;

import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.MESSAGES;
import static io.github.chechelpo.frplm.jooq.generated.Tables.SESSIONS;

public class MessageFixtures extends EntityFixtures<MessagesRecord, MessageService> {
    private final SessionFixtures sessionFixtures;
    MessageFixtures(MessageService service, EntityFixtureFactory fixtures, @NonNull String seed) {
        super(service, fixtures, seed);
        sessionFixtures = fixtures.sessions(seed);
    }

    @Override
    protected Set<TableField<MessagesRecord, ?>> doNotGenerateFields() {
        return Set.of(MESSAGES.TICK_NUM,MESSAGES.RESPONSE_NUM, MESSAGES.ACTIVE_RESPONSE, MESSAGES.REQUEST_JSON);
    }

    @Override
    protected DoActions<MessagesRecord> getFunctionsToAssignForeignFields(EntityDataPayload<MessagesRecord> sample) {
        DoActions<MessagesRecord> actions = DoActions.instantiate(1);

        sample.getAssignment(MESSAGES.SESSION_ID)
                .ifUnassignedRun(
                        () -> {
                            SessionsRecord session = sessionFixtures.createOne(
                                    EntityDataPayload.<SessionsRecord>builder()
                                            .copyIfAssigned(SESSIONS.WORLD_ID, MESSAGES.WORLD_ID, sample)
                                            .build()
                            );
                            actions.add(
                                    payload ->
                                            payload.ifUnassignedSet(MESSAGES.SESSION_ID, session.getId())
                            );
                        }
                );

        return actions;
    }
}
