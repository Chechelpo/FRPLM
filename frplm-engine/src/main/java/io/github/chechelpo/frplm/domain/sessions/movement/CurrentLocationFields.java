package io.github.chechelpo.frplm.domain.sessions.movement;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldType;
import io.github.chechelpo.frplm.core.entities.pseudo_services.ABSHelper;
import chechelpo.frplm.jooq.generated.tables.records.CurrentLocationsRecord;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.CURRENT_LOCATIONS;

@Component
final class CurrentLocationFields extends ABSHelper<CurrentLocationsRecord, CurrentLocationService> {
    CurrentLocationFields(CurrentLocationService service) {
        super(service);
        register_field(
                CURRENT_LOCATIONS.SESSION_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .require()
                        .build()
        );

        register_field(
                CURRENT_LOCATIONS.CHARACTER_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .require()
                        .build()
        );

        register_field(
                CURRENT_LOCATIONS.TICK_NUM,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER))
                        .require()
                        .build()
        );

        register_field(
                CURRENT_LOCATIONS.WORLD_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .build()
        );

        register_field(
                CURRENT_LOCATIONS.LOCATION_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .build()
        );
    }
}
