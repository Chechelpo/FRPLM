package chechelpo.frplm.domain.world.core;

import chechelpo.frplm.core.entities.fields.coercers.NumberCoercer;
import chechelpo.frplm.core.entities.pseudo_services.ABSControllerAwareHelper;
import chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import chechelpo.frplm.core.entities.fields.FieldInfo;
import chechelpo.frplm.core.entities.fields.kinds.FieldType;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.WORLDS;

@Component
public final class WorldFieldsHelper extends ABSControllerAwareHelper<
        WorldsRecord,
        WorldService,
        WorldController
        > {
    WorldFieldsHelper(
            WorldService service,
            WorldController controller
    ) {
        super(service, controller);

        register_field(
                "id",
                WORLDS.ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(
                                NumberConstraint.builder(FieldType.INTEGER)
                                        .key()
                                        .readOnly()
                                        .build()
                        )
                        .setFormat(NumberCoercer.create(FieldType.INTEGER))
                        .build()
        );

        register_field(
                "name",
                WORLDS.NAME,
                FieldInfo.stringField()
                        .setConstraints(
                                StringConstraint.builder()
                                        .setMaxLength(255)
                                        .build()
                        )
                        .build()
        );

        register_field(
                "lorebook_id",
                WORLDS.LOREBOOK_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .build()
        );

        register_field(
                null,
                WORLDS.NEXT_LOCATION_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .build()
        );
    }
}
