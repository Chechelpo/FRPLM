package io.github.chechelpo.frplm.domain.sessions.movement;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityFieldsValidator;
import io.github.chechelpo.frplm.jooq.generated.tables.records.MovementsRecord;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.MOVEMENTS;

@Component
final class MovementFieldsHelper
        extends EntityFieldsValidator<MovementsRecord> {

    MovementFieldsHelper() {
        super();
    }

    @Override
    protected List<FieldInfo<MovementsRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(MOVEMENTS.SESSION_ID)
                        .key()
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(MOVEMENTS.AT_TICK)
                        .key()
                        .build(),

                FieldInfo.builder(MOVEMENTS.CHARACTER_ID)
                        .key()
                        .build(),

                FieldInfo.builder(MOVEMENTS.WORLD_ID)
                        .readOnly()
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(MOVEMENTS.PREVIOUS_LOCATION_ID)
                        .requireOnCreate()
                        .build()
        );
    }
}