package io.github.chechelpo.frplm.domain.sessions.movementHistory;

import io.github.chechelpo.frplm.core.entities.fields.EntityFieldsValidator;
import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.jooq.generated.tables.records.ResponseLocationChangesRecord;
import org.jooq.Table;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.RESPONSE_LOCATION_CHANGES;

@Component
final class ResponseMovementFields extends EntityFieldsValidator<ResponseLocationChangesRecord> {
    ResponseMovementFields() {
        super(RESPONSE_LOCATION_CHANGES);
    }
    @Override
    protected List<FieldInfo<ResponseLocationChangesRecord, ?>> getCustom() {
        return List.of();
    }
}
