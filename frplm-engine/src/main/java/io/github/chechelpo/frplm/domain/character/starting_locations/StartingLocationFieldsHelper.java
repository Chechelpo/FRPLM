package io.github.chechelpo.frplm.domain.character.starting_locations;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.IntegerConstraint;
import io.github.chechelpo.frplm.core.entities.fields.EntityControllerFieldValidator;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.StartingLocationsRecord;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.tables.StartingLocations.STARTING_LOCATIONS;

@Component
public final class StartingLocationFieldsHelper
        extends EntityControllerFieldValidator<StartingLocationsRecord> {

    StartingLocationFieldsHelper() {
        super(EntityConfigs.Types.STARTING_LOCATIONS);
    }

    @Override
    protected List<FieldInfo<StartingLocationsRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(STARTING_LOCATIONS.WORLD_ID)
                        .requireOnCreate()
                        .key()
                        .build(),

                FieldInfo.builder(STARTING_LOCATIONS.CHARACTER_ID)
                        .requireOnCreate()
                        .key()
                        .build(),

                FieldInfo.builder(STARTING_LOCATIONS.LOCATION_ID)
                        .requireOnCreate()
                        .key()
                        .build(),

                FieldInfo.builder(STARTING_LOCATIONS.REASON_WHY)
                        .build(),

                FieldInfo.builder(STARTING_LOCATIONS.IS_STATIC)
                        .build(),

                FieldInfo.builder(STARTING_LOCATIONS.TTL)
                        .setConstraints(
                                IntegerConstraint.builder()
                                        .setMin(0)
                        )
                        .build()
        );
    }

    @Contract(" -> new")
    @Override
    protected @NonNull @Unmodifiable List<DTOField<StartingLocationsRecord, ?>> getDTOStructure() {
        return List.of(
                DTOField.of(STARTING_LOCATIONS.WORLD_ID, "worldID"),
                DTOField.of(STARTING_LOCATIONS.CHARACTER_ID, "characterID"),
                DTOField.of(STARTING_LOCATIONS.LOCATION_ID, "locationID"),
                DTOField.of(STARTING_LOCATIONS.REASON_WHY, "reason_why"),
                DTOField.of(STARTING_LOCATIONS.IS_STATIC, "is_static"),
                DTOField.of(STARTING_LOCATIONS.TTL, "ttl")
        );
    }
}