package io.github.chechelpo.frplm.domain.sessions.movement;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityControllerFieldValidator;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityFieldsValidator;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CurrentLocationsRecord;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.CURRENT_LOCATIONS;

@Component
final class CurrentLocationFields
        extends EntityFieldsValidator<CurrentLocationsRecord> {

    CurrentLocationFields() {
        super();
    }

    @Override
    protected List<FieldInfo<CurrentLocationsRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(CURRENT_LOCATIONS.SESSION_ID)
                        .key()
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(CURRENT_LOCATIONS.CHARACTER_ID)
                        .key()
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(CURRENT_LOCATIONS.TICK_NUM)
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(CURRENT_LOCATIONS.WORLD_ID)
                        .build(),

                FieldInfo.builder(CURRENT_LOCATIONS.LOCATION_ID)
                        .build()
        );
    }
}