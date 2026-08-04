package io.github.chechelpo.frplm.domain.world.core;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityControllerFieldValidator;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.WORLDS;

@Component
public final class WorldFieldsHelper
        extends EntityControllerFieldValidator<WorldsRecord> {

    WorldFieldsHelper() {
        super(EntityConfigs.Types.WORLDS);
    }

    @Override
    protected List<DTOField<WorldsRecord, ?>> getDTOStructure() {
        return List.of(
                DTOField.of(WORLDS.ID, "id"),
                DTOField.of(WORLDS.NAME, "name"),
                DTOField.of(WORLDS.DESCRIPTION, "description"),
                DTOField.of(WORLDS.LOREBOOK_ID, "lorebook_id")
        );
    }

    @Override
    protected List<FieldInfo<WorldsRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(WORLDS.ID)
                        .key()
                        .build(),

                FieldInfo.builder(WORLDS.NAME)
                        .setConstraints(
                                StringConstraint.builder()
                                        .setMaxLength(255)
                        )
                        .build(),

                FieldInfo.builder(WORLDS.DESCRIPTION)
                        .build(),

                FieldInfo.builder(WORLDS.LOREBOOK_ID)
                        .build(),

                FieldInfo.builder(WORLDS.NEXT_LOCATION_ID)
                        .build(),

                FieldInfo.builder(WORLDS.NEXT_REGION_ID)
                        .build()
        );
    }
}