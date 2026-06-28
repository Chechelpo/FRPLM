package chechelpo.frplm.domain.world.region;

import chechelpo.frplm.core.entities.fields.FieldInfo;
import chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import chechelpo.frplm.core.entities.fields.kinds.FieldType;
import chechelpo.frplm.core.entities.pseudo_services.ABSControllerAwareHelper;
import chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.REGION;

@Component
final class RegionFields extends ABSControllerAwareHelper<RegionRecord, RegionService, RegionController> {
    RegionFields(RegionService service, RegionController controller) {
        super(service, controller);

        register_field(
                "world_id",
                REGION.WORLD_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                                .build()
                        )
                        .build()
        );
        register_field(
                "id",
                REGION.ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                                .build()
                        )
                        .build()
        );
        register_field(
                "parent_region_id",
                REGION.PARENT_REGION_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .nullable()
                                .build()
                        )
                        .build()
        );

        register_field(
                "lorebook_id",
                REGION.LOREBOOK_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .readOnly()
                        )
                        .build()
        );

        register_field(
                "name",
                REGION.NAME,
                FieldInfo.stringField()
                        .require()
                        .build()
        );
        register_field(
                "description",
                REGION.DESCRIPTION,
                FieldInfo.stringField()
                        .build()
        );
    }
}
