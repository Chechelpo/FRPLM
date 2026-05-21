package chechelpo.frplm.domain.world.core.microservices;

import chechelpo.frplm.frameworks.entities.microservices.ABSControllerAwareHelper;
import chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import chechelpo.frplm.frameworks.entities.fields.constraints.NumberConstraints;
import chechelpo.frplm.frameworks.entities.fields.constraints.StringConstraints;
import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
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
                                NumberConstraints.builder(FieldType.INTEGER)
                                        .key()
                                        .readOnly()
                                        .build()
                        )
                        .build()
        );

        register_field(
                "name",
                WORLDS.NAME,
                FieldInfo.stringField()
                        .setConstraints(
                                StringConstraints.builder()
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
    }
}
