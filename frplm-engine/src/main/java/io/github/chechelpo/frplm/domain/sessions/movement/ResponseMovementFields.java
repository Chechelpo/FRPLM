package io.github.chechelpo.frplm.domain.sessions.movement;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.EntityFieldsValidator;
import io.github.chechelpo.frplm.jooq.generated.tables.records.ResponseLocationChangesRecord;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.RESPONSE_LOCATION_CHANGES;

@Component
public class ResponseMovementFields
        extends EntityFieldsValidator<ResponseLocationChangesRecord> {

    public ResponseMovementFields() {
        super();
    }

    @Override
    protected List<FieldInfo<ResponseLocationChangesRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(RESPONSE_LOCATION_CHANGES.SESSION_ID)
                        .key()
                        .build(),

                FieldInfo.builder(RESPONSE_LOCATION_CHANGES.TICK_NUM)
                        .key()
                        .build(),

                FieldInfo.builder(RESPONSE_LOCATION_CHANGES.RESPONSE_NUM)
                        .key()
                        .build(),

                FieldInfo.builder(RESPONSE_LOCATION_CHANGES.CHARACTER_ID)
                        .key()
                        .build(),

                FieldInfo.builder(RESPONSE_LOCATION_CHANGES.WORLD_ID)
                        .build(),

                FieldInfo.builder(RESPONSE_LOCATION_CHANGES.LOCATION_ID)
                        .build()
        );
    }
}