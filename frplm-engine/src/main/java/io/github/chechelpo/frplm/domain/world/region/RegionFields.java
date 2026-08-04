package io.github.chechelpo.frplm.domain.world.region;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityControllerFieldValidator;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.REGION;

@Component
final class RegionFields extends EntityControllerFieldValidator<RegionRecord> {

    RegionFields() {
        super(EntityConfigs.Types.REGIONS);
    }

    @Override
    protected List<DTOField<RegionRecord, ?>> getDTOStructure() {
        return List.of(
                DTOField.of(REGION.WORLD_ID, "world_id"),
                DTOField.of(REGION.ID, "id"),
                DTOField.of(REGION.PARENT_REGION_ID, "parent_region_id"),
                DTOField.of(REGION.LOREBOOK_ID, "lorebook_id"),
                DTOField.of(REGION.NAME, "name"),
                DTOField.of(REGION.DESCRIPTION, "description")
        );
    }

    @Override
    protected List<FieldInfo<RegionRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(REGION.WORLD_ID)
                        .key()
                        .build(),

                FieldInfo.builder(REGION.ID)
                        .key()
                        .build(),

                FieldInfo.builder(REGION.PARENT_REGION_ID)
                        .nullable()
                        .build(),

                FieldInfo.builder(REGION.LOREBOOK_ID)
                        .readOnly()
                        .build(),

                FieldInfo.builder(REGION.NAME)
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(REGION.DESCRIPTION)
                        .build()
        );
    }
}