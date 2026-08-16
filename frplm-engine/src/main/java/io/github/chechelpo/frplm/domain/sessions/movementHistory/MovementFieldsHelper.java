package io.github.chechelpo.frplm.domain.sessions.movementHistory;

import io.github.chechelpo.frplm.core.entities.fields.EntityFieldsValidator;
import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.jooq.generated.tables.records.MovementsRecord;
import org.jooq.Table;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.MOVEMENTS;

@Component
final class MovementFieldsHelper extends EntityFieldsValidator<MovementsRecord> {
    MovementFieldsHelper() {
        super(MOVEMENTS);
    }

    @Override
    protected List<FieldInfo<MovementsRecord, ?>> getCustom() {
        return List.of();
    }
}
