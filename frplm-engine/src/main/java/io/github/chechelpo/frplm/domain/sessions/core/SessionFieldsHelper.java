package io.github.chechelpo.frplm.domain.sessions.core;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.EntityControllerFieldValidator;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.SESSIONS;

@Component
final class SessionFieldsHelper extends EntityControllerFieldValidator<SessionsRecord> {
    SessionFieldsHelper() {
        super(EntityConfigs.Types.SESSIONS, SESSIONS);
    }

    @Override
    protected List<DTOField<SessionsRecord, ?>> getDTOStructure() {
        return List.of(
                DTOField.of(SESSIONS.ID, "id"),
                DTOField.of(SESSIONS.CURRENT_TICK, "current_tick"),
                DTOField.of(SESSIONS.NAME, "name"),
                DTOField.of(SESSIONS.USER_PERSONA_ID, "user_id"),
                DTOField.of(SESSIONS.WORLD_ID, "world_id"),
                DTOField.of(SESSIONS.PROMPT_ID, "template_id")
        );
    }

    @Override
    protected List<FieldInfo<SessionsRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(SESSIONS.ID)
                        .key()
                        .build(),

                FieldInfo.builder(SESSIONS.CURRENT_TICK)
                        .setDefaultValue(0)
                        .build(),

                FieldInfo.builder(SESSIONS.NAME)
                        .build(),

                FieldInfo.builder(SESSIONS.USER_PERSONA_ID)
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(SESSIONS.WORLD_ID)
                        .readOnly()
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(SESSIONS.PROMPT_ID)
                        .nullable()
                        .build()
        );
    }
}